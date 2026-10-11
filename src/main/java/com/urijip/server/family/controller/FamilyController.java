package com.urijip.server.family.controller;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.response.ApiResponse;
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
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController {

	private final FamilyService familyService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FamilyResponse> create(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody FamilyCreateRequest request) {
		return ApiResponse.success(familyService.create(userId, request));
	}

	@PostMapping("/join")
	public ApiResponse<FamilyResponse> join(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody FamilyJoinRequest request) {
		return ApiResponse.success(familyService.join(userId, request));
	}

}
