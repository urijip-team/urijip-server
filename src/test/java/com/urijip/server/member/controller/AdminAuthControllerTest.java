package com.urijip.server.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.member.dto.request.LoginRequest;
import com.urijip.server.member.dto.response.AdminLoginResponse;
import com.urijip.server.member.entity.AdminLevel;
import com.urijip.server.member.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(AdminAuthController.class)
@Import(SecurityConfig.class)
class AdminAuthControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void loginReturnsTokensAndAdminWithoutAccessToken() {
		given(authService.adminLogin(new LoginRequest("admin@example.com", "password123")))
				.willReturn(new AdminLoginResponse("access-token", "refresh-token",
						new AdminLoginResponse.AdminInfo(1L, "관리자", AdminLevel.SUPER)));

		MvcTestResult result = login("""
				{"email": "admin@example.com", "password": "password123"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.accessToken").isEqualTo("access-token");
		assertThat(result).bodyJson().extractingPath("$.data.refreshToken").isEqualTo("refresh-token");
		assertThat(result).bodyJson().extractingPath("$.data.admin.id").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data.admin.name").isEqualTo("관리자");
		assertThat(result).bodyJson().extractingPath("$.data.admin.adminLevel").isEqualTo("SUPER");
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	private MvcTestResult login(String body) {
		return mvc.post().uri("/admin/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

}
