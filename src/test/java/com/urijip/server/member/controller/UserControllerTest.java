package com.urijip.server.member.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.member.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AuthService authService;

	@Test
	void withdrawReturnsOkWithoutData() {
		MvcTestResult result = withdraw(1L);

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data").isNull();
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	private MvcTestResult withdraw(Long userId) {
		return mvc.delete().uri("/api/users/me")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.exchange();
	}

}
