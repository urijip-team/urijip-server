package com.urijip.server.report.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.report.dto.request.ReportCreateRequest;
import com.urijip.server.report.dto.response.ReportResponse;
import com.urijip.server.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController {

	private final ReportService reportService;

	@PostMapping("/api/reports")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ReportResponse> create(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody ReportCreateRequest request) {
		return ApiResponse.success(reportService.create(userId, request));
	}

}
