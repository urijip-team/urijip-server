package com.urijip.server.allowance.controller;

import com.urijip.server.allowance.dto.request.AllowanceCreateRequest;
import com.urijip.server.allowance.dto.response.AllowanceCreateResponse;
import com.urijip.server.allowance.dto.response.AllowanceDetailResponse;
import com.urijip.server.allowance.service.AllowanceService;
import com.urijip.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/allowance-requests")
@RequiredArgsConstructor
public class AllowanceController {

	private final AllowanceService allowanceService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AllowanceCreateResponse> create(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody AllowanceCreateRequest request) {
		return ApiResponse.success(allowanceService.create(userId, request));
	}

	@GetMapping("/{requestId}")
	public ApiResponse<AllowanceDetailResponse> getDetail(@AuthenticationPrincipal Long userId,
			@PathVariable Long requestId) {
		return ApiResponse.success(allowanceService.getDetail(requestId, userId));
	}

}
