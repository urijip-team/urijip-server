package com.urijip.server.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.dto.request.LoginRequest;
import com.urijip.server.member.dto.request.SignupRequest;
import com.urijip.server.member.dto.response.LoginResponse;
import com.urijip.server.member.dto.response.SignupResponse;
import com.urijip.server.member.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void signupReturnsCreatedWithUser() {
		given(authService.signup(new SignupRequest("mom@example.com", "password123", "김엄마")))
				.willReturn(new SignupResponse(1L, "mom@example.com", "김엄마"));

		MvcTestResult result = signup("""
				{"email": "mom@example.com", "password": "password123", "name": "김엄마"}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data.email").isEqualTo("mom@example.com");
		assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo("김엄마");
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	@Test
	void signupDoesNotExposePassword() {
		given(authService.signup(any()))
				.willReturn(new SignupResponse(1L, "mom@example.com", "김엄마"));

		MvcTestResult result = signup("""
				{"email": "mom@example.com", "password": "password123", "name": "김엄마"}
				""");

		assertThat(result).bodyText().doesNotContain("password");
	}

	@Test
	void signupWithDuplicatedEmailReturnsConflict() {
		given(authService.signup(any())).willThrow(new BusinessException(ErrorCode.EMAIL_DUPLICATED));

		MvcTestResult result = signup("""
				{"email": "mom@example.com", "password": "password123", "name": "김엄마"}
				""");

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("EMAIL_DUPLICATED");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			email    | {"email": "not-an-email", "password": "password123", "name": "김엄마"}
			email    | {"email": "", "password": "password123", "name": "김엄마"}
			password | {"email": "mom@example.com", "password": "short", "name": "김엄마"}
			name     | {"email": "mom@example.com", "password": "password123", "name": " "}
			name     | {"email": "mom@example.com", "password": "password123"}
			""")
	void signupWithInvalidInputReturnsBadRequest(String field, String body) {
		MvcTestResult result = signup(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains(field + ": ");
		then(authService).should(never()).signup(any());
	}

	@Test
	void signupWithShortPasswordExplainsMinimumLength() {
		MvcTestResult result = signup("""
				{"email": "mom@example.com", "password": "short", "name": "김엄마"}
				""");

		assertThat(result).bodyJson().extractingPath("$.error.message")
				.isEqualTo("password: 8자 이상이어야 합니다");
	}

	@Test
	void loginReturnsTokensAndUser() {
		given(authService.login(new LoginRequest("mom@example.com", "password123")))
				.willReturn(new LoginResponse("access-token", "refresh-token",
						new LoginResponse.UserInfo(1L, "김엄마", false)));

		MvcTestResult result = login("""
				{"email": "mom@example.com", "password": "password123"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.accessToken").isEqualTo("access-token");
		assertThat(result).bodyJson().extractingPath("$.data.refreshToken").isEqualTo("refresh-token");
		assertThat(result).bodyJson().extractingPath("$.data.user.id").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data.user.name").isEqualTo("김엄마");
		assertThat(result).bodyJson().extractingPath("$.data.user.hasFamily").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	private MvcTestResult signup(String body) {
		return mvc.post().uri("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult login(String body) {
		return mvc.post().uri("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

}
