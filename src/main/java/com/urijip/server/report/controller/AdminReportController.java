package com.urijip.server.report.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.report.dto.request.AdminReportHandleRequest;
import com.urijip.server.report.dto.request.AdminReportSearchRequest;
import com.urijip.server.report.dto.response.AdminReportResponse;
import com.urijip.server.report.service.AdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

	private final AdminReportService adminReportService;

	@GetMapping
	public ApiResponse<PageResponse<AdminReportResponse>> search(@ModelAttribute AdminReportSearchRequest request) {
		return ApiResponse.success(adminReportService.search(request));
	}

	@PatchMapping("/{reportId}")
	public ApiResponse<AdminReportResponse> handle(@AuthenticationPrincipal Long adminId,
			@PathVariable Long reportId, @Valid @RequestBody AdminReportHandleRequest request) {
		return ApiResponse.success(adminReportService.handle(reportId, adminId, request));
	}

}
