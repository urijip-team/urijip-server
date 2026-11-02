package com.urijip.server.sos.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.sos.dto.request.SosSearchRequest;
import com.urijip.server.sos.dto.request.SosSendRequest;
import com.urijip.server.sos.dto.response.SosCreateResponse;
import com.urijip.server.sos.dto.response.SosResponse;
import com.urijip.server.sos.service.SosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SosController {

	private final SosService sosService;

	@PostMapping("/api/sos")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SosCreateResponse> send(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody SosSendRequest request) {
		return ApiResponse.success(sosService.send(userId, request));
	}

	@PatchMapping("/api/sos/{sosId}/resolve")
	public ApiResponse<SosResponse> resolve(@AuthenticationPrincipal Long userId, @PathVariable Long sosId) {
		return ApiResponse.success(sosService.resolve(sosId, userId));
	}

	@GetMapping("/api/families/{familyId}/sos")
	public ApiResponse<PageResponse<SosResponse>> getHistory(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId, @ModelAttribute SosSearchRequest request) {
		return ApiResponse.success(sosService.getHistory(familyId, userId, request));
	}

}
