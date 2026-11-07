package com.urijip.server.notice.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.notice.dto.request.AdminNoticeCreateRequest;
import com.urijip.server.notice.dto.request.AdminNoticeUpdateRequest;
import com.urijip.server.notice.dto.response.AdminNoticeResponse;
import com.urijip.server.notice.service.AdminNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

	private final AdminNoticeService adminNoticeService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminNoticeResponse> create(@AuthenticationPrincipal Long adminId,
			@Valid @RequestBody AdminNoticeCreateRequest request) {
		return ApiResponse.success(adminNoticeService.create(adminId, request));
	}

	@PatchMapping("/{noticeId}")
	public ApiResponse<AdminNoticeResponse> update(@PathVariable Long noticeId,
			@Valid @RequestBody AdminNoticeUpdateRequest request) {
		return ApiResponse.success(adminNoticeService.update(noticeId, request));
	}

}
