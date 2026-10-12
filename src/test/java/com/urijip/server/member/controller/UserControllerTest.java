package com.urijip.server.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.member.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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

	@Test
	void withdrawPassesUserIdOfAccessToken() {
		withdraw(7L);

		then(authService).should().withdraw(7L);
	}

	@Test
	void withdrawOfOwnerWithOtherMembersReturnsConflict() {
		willThrow(new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE)).given(authService).withdraw(1L);

		MvcTestResult result = withdraw(1L);

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("OWNER_CANNOT_LEAVE");
	}

	@Test
	void withdrawWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.delete().uri("/api/users/me").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(authService).should(never()).withdraw(any());
	}

	private MvcTestResult withdraw(Long userId) {
		return mvc.delete().uri("/api/users/me")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.exchange();
	}

}
