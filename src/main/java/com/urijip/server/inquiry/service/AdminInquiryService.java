package com.urijip.server.inquiry.service;

import com.urijip.server.global.response.PageResponse;
import com.urijip.server.inquiry.dto.request.AdminInquirySearchRequest;
import com.urijip.server.inquiry.dto.response.AdminInquiryResponse;
import com.urijip.server.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminInquiryService {

	private final InquiryRepository inquiryRepository;

	@Transactional(readOnly = true)
	public PageResponse<AdminInquiryResponse> search(AdminInquirySearchRequest request) {
		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault());
		return PageResponse.from(inquiryRepository.search(request.answered(), pageable)
				.map(AdminInquiryResponse::from));
	}

}
