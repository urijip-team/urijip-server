package com.urijip.server.inquiry.service;

import com.urijip.server.adminlog.entity.AdminAction;
import com.urijip.server.adminlog.entity.AdminLogTargetType;
import com.urijip.server.adminlog.service.AdminLogService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.inquiry.dto.request.AdminInquiryAnswerRequest;
import com.urijip.server.inquiry.dto.request.AdminInquirySearchRequest;
import com.urijip.server.inquiry.dto.response.AdminInquiryResponse;
import com.urijip.server.inquiry.entity.Inquiry;
import com.urijip.server.inquiry.repository.InquiryRepository;
import com.urijip.server.member.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminInquiryService {

	private final InquiryRepository inquiryRepository;
	private final UserService userService;
	private final AdminLogService adminLogService;

	@Transactional(readOnly = true)
	public PageResponse<AdminInquiryResponse> search(AdminInquirySearchRequest request) {
		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault());
		return PageResponse.from(inquiryRepository.search(request.answered(), pageable)
				.map(AdminInquiryResponse::from));
	}

	// 답변은 한 번만 할 수 있다.
	@Transactional
	public AdminInquiryResponse answer(Long inquiryId, Long adminId, AdminInquiryAnswerRequest request) {
		Inquiry inquiry = inquiryRepository.findById(inquiryId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
		if (inquiry.isAnswered()) {
			throw new BusinessException(ErrorCode.INQUIRY_ALREADY_ANSWERED);
		}

		inquiry.answer(userService.getUser(adminId), request.answer());
		adminLogService.record(adminId, AdminAction.ANSWER_INQUIRY, AdminLogTargetType.INQUIRY, inquiryId,
				inquiry.getTitle());
		return AdminInquiryResponse.from(inquiry);
	}

}
