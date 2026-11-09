package com.urijip.server.adminlog.controller;

import com.urijip.server.adminlog.dto.request.AdminLogSearchRequest;
import com.urijip.server.adminlog.dto.response.AdminLogResponse;
import com.urijip.server.adminlog.service.AdminLogService;
import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

	private final AdminLogService adminLogService;

	@GetMapping
	public ApiResponse<PageResponse<AdminLogResponse>> search(@ModelAttribute AdminLogSearchRequest request) {
		return ApiResponse.success(adminLogService.search(request));
	}

}
