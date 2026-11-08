package com.urijip.server.stats.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.stats.dto.request.AdminStatsRequest;
import com.urijip.server.stats.dto.response.AdminStatsResponse;
import com.urijip.server.stats.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

	private final AdminStatsService adminStatsService;

	@GetMapping
	public ApiResponse<AdminStatsResponse> getStats(@ModelAttribute AdminStatsRequest request) {
		return ApiResponse.success(adminStatsService.getStats(request));
	}

}
