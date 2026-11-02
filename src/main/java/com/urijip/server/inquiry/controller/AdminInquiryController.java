package com.urijip.server.inquiry.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.inquiry.dto.request.AdminInquiryAnswerRequest;
import com.urijip.server.inquiry.dto.request.AdminInquirySearchRequest;
import com.urijip.server.inquiry.dto.response.AdminInquiryResponse;
import com.urijip.server.inquiry.service.AdminInquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

	private final AdminInquiryService adminInquiryService;

	@GetMapping
	public ApiResponse<PageResponse<AdminInquiryResponse>> search(@ModelAttribute AdminInquirySearchRequest request) {
		return ApiResponse.success(adminInquiryService.search(request));
	}

	@PostMapping("/{inquiryId}/answer")
	public ApiResponse<AdminInquiryResponse> answer(@AuthenticationPrincipal Long adminId,
			@PathVariable Long inquiryId, @Valid @RequestBody AdminInquiryAnswerRequest request) {
		return ApiResponse.success(adminInquiryService.answer(inquiryId, adminId, request));
	}

}
