package com.urijip.server.notification.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.notification.dto.request.NotificationSettingUpdateRequest;
import com.urijip.server.notification.dto.response.NotificationSettingResponse;
import com.urijip.server.notification.service.NotificationSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingController {

	private final NotificationSettingService notificationSettingService;

	@GetMapping
	public ApiResponse<NotificationSettingResponse> getMySettings(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(notificationSettingService.getSettings(userId));
	}

	@PatchMapping
	public ApiResponse<NotificationSettingResponse> updateMySettings(@AuthenticationPrincipal Long userId,
			@RequestBody NotificationSettingUpdateRequest request) {
		return ApiResponse.success(notificationSettingService.update(userId, request));
	}

}
