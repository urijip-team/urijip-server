package com.urijip.server.member.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.member.dto.request.DeviceTokenRequest;
import com.urijip.server.member.dto.request.ProfileUpdateRequest;
import com.urijip.server.member.dto.response.ProfileResponse;
import com.urijip.server.member.service.AuthService;
import com.urijip.server.member.service.DeviceTokenService;
import com.urijip.server.member.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final ProfileService profileService;

	@GetMapping("/{userId}")
	public ApiResponse<ProfileResponse> getProfile(@AuthenticationPrincipal Long userId,
			@PathVariable("userId") Long targetUserId) {
		return ApiResponse.success(profileService.getProfile(userId, targetUserId));
	}

	@PatchMapping("/me")
	public ApiResponse<ProfileResponse> updateMyProfile(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody ProfileUpdateRequest request) {
		return ApiResponse.success(profileService.updateMyProfile(userId, request));
	}

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
