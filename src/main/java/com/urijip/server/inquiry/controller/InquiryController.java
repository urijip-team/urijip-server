package com.urijip.server.inquiry.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.inquiry.dto.request.InquiryCreateRequest;
import com.urijip.server.inquiry.dto.request.InquirySearchRequest;
import com.urijip.server.inquiry.dto.response.InquiryResponse;
import com.urijip.server.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

	private final InquiryService inquiryService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<InquiryResponse> create(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody InquiryCreateRequest request) {
		return ApiResponse.success(inquiryService.create(userId, request));
	}

	@GetMapping
	public ApiResponse<PageResponse<InquiryResponse>> getMyInquiries(@AuthenticationPrincipal Long userId,
			@ModelAttribute InquirySearchRequest request) {
		return ApiResponse.success(inquiryService.getMyInquiries(userId, request));
	}

}
