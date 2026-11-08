package com.urijip.server.inquiry.service;

import com.urijip.server.global.response.PageResponse;
import com.urijip.server.inquiry.dto.request.InquiryCreateRequest;
import com.urijip.server.inquiry.dto.request.InquirySearchRequest;
import com.urijip.server.inquiry.dto.response.InquiryResponse;
import com.urijip.server.inquiry.entity.Inquiry;
import com.urijip.server.inquiry.repository.InquiryRepository;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

	private final InquiryRepository inquiryRepository;
	private final UserService userService;

	@Transactional
	public InquiryResponse create(Long userId, InquiryCreateRequest request) {
		User user = userService.getUser(userId);
		Inquiry inquiry = inquiryRepository.save(new Inquiry(user, request.title(), request.content()));
		return InquiryResponse.from(inquiry);
	}

	@Transactional(readOnly = true)
	public PageResponse<InquiryResponse> getMyInquiries(Long userId, InquirySearchRequest request) {
		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(),
				Sort.by(Sort.Direction.DESC, "id"));
		return PageResponse.from(inquiryRepository.findByUserId(userId, pageable).map(InquiryResponse::from));
	}

	@Transactional(readOnly = true)
	public long countUnanswered() {
		return inquiryRepository.countByAnsweredAtIsNull();
	}

}
