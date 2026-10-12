package com.urijip.server.member.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.member.dto.request.DeviceTokenRequest;
import com.urijip.server.member.service.AuthService;
import com.urijip.server.member.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final AuthService authService;
	private final DeviceTokenService deviceTokenService;

	@DeleteMapping("/me")
	public ApiResponse<Void> withdraw(@AuthenticationPrincipal Long userId) {
		authService.withdraw(userId);
		return ApiResponse.success(null);
	}

	@PutMapping("/me/device-token")
	public ApiResponse<Void> registerDeviceToken(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody DeviceTokenRequest request) {
		deviceTokenService.register(userId, request);
		return ApiResponse.success(null);
	}

}
