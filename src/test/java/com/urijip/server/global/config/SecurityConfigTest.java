package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.security.JwtProperties;
import com.urijip.server.global.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityConfigTest.TestController.class)
@Import({SecurityConfigTest.TestController.class, SecurityConfig.class})
class SecurityConfigTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private JwtProperties jwtProperties;

	@Test
	void requestWithoutTokenIsUnauthorized() {
		assertError(get("/api/test/me"), HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
	}

	@Test
	void validTokenReachesControllerWithUserId() {
		MvcTestResult result = getWithToken("/api/test/me", jwtProvider.createAccessToken(1L, "USER"));

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data").isEqualTo(1);
	}

	private MvcTestResult get(String uri) {
		return mvc.get().uri(uri).exchange();
	}

	private MvcTestResult getWithToken(String uri, String token) {
		return mvc.get().uri(uri).header("Authorization", "Bearer " + token).exchange();
	}

	private static void assertError(MvcTestResult result, HttpStatus status, String code) {
		assertThat(result).hasStatus(status);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo(code);
	}

	@RestController
	static class TestController {

		@GetMapping("/api/test/me")
		ApiResponse<Long> me(@AuthenticationPrincipal Long userId) {
			return ApiResponse.success(userId);
		}

		@GetMapping("/admin/test")
		ApiResponse<String> admin() {
			return ApiResponse.success("admin");
		}

	}

}
