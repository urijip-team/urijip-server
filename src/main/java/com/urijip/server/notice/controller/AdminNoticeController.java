package com.urijip.server.notice.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.notice.dto.request.AdminNoticeCreateRequest;
import com.urijip.server.notice.dto.response.AdminNoticeResponse;
import com.urijip.server.notice.service.AdminNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

}
