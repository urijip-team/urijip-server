package com.urijip.server.inquiry.service;

import java.util.List;
import java.util.Map;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.push.PushMessage;
import com.urijip.server.global.push.PushType;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.inquiry.dto.request.AdminInquiryAnswerRequest;
import com.urijip.server.inquiry.dto.request.AdminInquirySearchRequest;
import com.urijip.server.inquiry.dto.response.AdminInquiryResponse;
import com.urijip.server.inquiry.entity.Inquiry;
import com.urijip.server.inquiry.repository.InquiryRepository;
import com.urijip.server.member.service.UserService;
import com.urijip.server.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminInquiryService {

	private final InquiryRepository inquiryRepository;
	private final UserService userService;
	private final NotificationService notificationService;

	@Transactional(readOnly = true)
	public PageResponse<AdminInquiryResponse> search(AdminInquirySearchRequest request) {
		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault());
		return PageResponse.from(inquiryRepository.search(request.answered(), pageable)
				.map(AdminInquiryResponse::from));
	}

	// 답변은 한 번만 할 수 있다. 답변하면 문의한 회원에게 푸시한다.
	@Transactional
	public AdminInquiryResponse answer(Long inquiryId, Long adminId, AdminInquiryAnswerRequest request) {
		Inquiry inquiry = inquiryRepository.findById(inquiryId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
		if (inquiry.isAnswered()) {
			throw new BusinessException(ErrorCode.INQUIRY_ALREADY_ANSWERED);
		}

		inquiry.answer(userService.getUser(adminId), request.answer());
		notificationService.send(List.of(inquiry.getUser().getId()), new PushMessage(PushType.INQUIRY, "문의 답변",
				"'" + inquiry.getTitle() + "' 문의에 답변이 등록됐어요.", Map.of("inquiryId", inquiry.getId().toString())));
		return AdminInquiryResponse.from(inquiry);
	}

}
