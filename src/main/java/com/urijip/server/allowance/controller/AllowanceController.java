package com.urijip.server.allowance.controller;

import java.time.YearMonth;

import com.urijip.server.allowance.dto.request.AllowanceCreateRequest;
import com.urijip.server.allowance.dto.request.AllowanceStatusRequest;
import com.urijip.server.allowance.dto.response.AllowanceCreateResponse;
import com.urijip.server.allowance.dto.response.AllowanceDetailResponse;
import com.urijip.server.allowance.dto.response.AllowanceMonthlyResponse;
import com.urijip.server.allowance.dto.response.AllowanceResponse;
import com.urijip.server.allowance.service.AllowanceService;
import com.urijip.server.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	// month는 2026-10 형식, type은 sent나 received다.
	@GetMapping
	public ApiResponse<AllowanceMonthlyResponse> getMonthly(@AuthenticationPrincipal Long userId,
			@RequestParam YearMonth month, @RequestParam String type) {
		return ApiResponse.success(allowanceService.getMonthly(userId, month, type));
	}

	@GetMapping("/{requestId}")
	public ApiResponse<AllowanceDetailResponse> getDetail(@AuthenticationPrincipal Long userId,
			@PathVariable Long requestId) {
		return ApiResponse.success(allowanceService.getDetail(requestId, userId));
	}

	@PatchMapping("/{requestId}")
	public ApiResponse<AllowanceResponse> changeStatus(@AuthenticationPrincipal Long userId,
			@PathVariable Long requestId, @Valid @RequestBody AllowanceStatusRequest request) {
		return ApiResponse.success(allowanceService.changeStatus(requestId, userId, request));
	}

}
