package com.urijip.server.member.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.member.dto.request.AdminUserSearchRequest;
import com.urijip.server.member.dto.request.AdminUserStatusRequest;
import com.urijip.server.member.dto.response.AdminUserDetailResponse;
import com.urijip.server.member.dto.response.AdminUserResponse;
import com.urijip.server.member.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final AdminUserService adminUserService;

	@GetMapping
	public ApiResponse<PageResponse<AdminUserResponse>> search(@ModelAttribute AdminUserSearchRequest request) {
		return ApiResponse.success(adminUserService.search(request));
	}

	@GetMapping("/{userId}")
	public ApiResponse<AdminUserDetailResponse> getUser(@PathVariable Long userId) {
		return ApiResponse.success(adminUserService.getUser(userId));
	}

	@PatchMapping("/{userId}/status")
	public ApiResponse<AdminUserDetailResponse> changeStatus(@PathVariable Long userId,
			@Valid @RequestBody AdminUserStatusRequest request) {
		return ApiResponse.success(adminUserService.changeStatus(userId, request));
	}

}
