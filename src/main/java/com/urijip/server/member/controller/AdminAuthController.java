package com.urijip.server.member.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.member.dto.request.LoginRequest;
import com.urijip.server.member.dto.response.AdminLoginResponse;
import com.urijip.server.member.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.adminLogin(request));
	}

}
