package com.urijip.server.allowance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.urijip.server.allowance.dto.request.AllowanceCreateRequest;
import com.urijip.server.allowance.dto.response.AllowanceCreateResponse;
import com.urijip.server.allowance.entity.AllowanceStatus;
import com.urijip.server.allowance.service.AllowanceService;
import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(AllowanceController.class)
@Import(SecurityConfig.class)
class AllowanceControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AllowanceService allowanceService;

	@Test
	void createReturnsCreated() {
		given(allowanceService.create(2L, new AllowanceCreateRequest(3L, 1L, 30000, "문제집 사야 해요")))
				.willReturn(new AllowanceCreateResponse(45L, AllowanceStatus.REQUESTED));

		MvcTestResult result = create("""
				{"familyId": 3, "receiverId": 1, "amount": 30000, "reason": "문제집 사야 해요"}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.requestId").isEqualTo(45);
		assertThat(result).bodyJson().extractingPath("$.data.status").isEqualTo("REQUESTED");
	}

	@Test
	void createWithoutReasonIsAllowed() {
		given(allowanceService.create(2L, new AllowanceCreateRequest(3L, 1L, 5000, null)))
				.willReturn(new AllowanceCreateResponse(46L, AllowanceStatus.REQUESTED));

		MvcTestResult result = create("{\"familyId\": 3, \"receiverId\": 1, \"amount\": 5000}");

		assertThat(result).hasStatus(HttpStatus.CREATED);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"receiverId\": 1, \"amount\": 5000}",
			"{\"familyId\": 3, \"amount\": 5000}",
			"{\"familyId\": 3, \"receiverId\": 1}",
			"{\"familyId\": 3, \"receiverId\": 1, \"amount\": 0}",
			"{\"familyId\": 3, \"receiverId\": 1, \"amount\": -100}",
			"{\"familyId\": 3, \"receiverId\": 1, \"amount\": 10000001}"
	})
	void createWithInvalidBodyReturnsBadRequest(String body) {
		MvcTestResult result = create(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(allowanceService).should(never()).create(any(), any());
	}

	@Test
	void createWithTooLongReasonReturnsBadRequest() {
		MvcTestResult result = create("{\"familyId\": 3, \"receiverId\": 1, \"amount\": 5000, \"reason\": \"%s\"}"
				.formatted("가".repeat(101)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createToSelfReturnsBadRequest() {
		given(allowanceService.create(2L, new AllowanceCreateRequest(3L, 2L, 5000, null)))
				.willThrow(new BusinessException(ErrorCode.CANNOT_REQUEST_SELF));

		MvcTestResult result = create("{\"familyId\": 3, \"receiverId\": 2, \"amount\": 5000}");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("CANNOT_REQUEST_SELF");
	}

	@Test
	void createInOtherFamilyReturnsForbidden() {
		given(allowanceService.create(2L, new AllowanceCreateRequest(9L, 1L, 5000, null)))
				.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = create("{\"familyId\": 9, \"receiverId\": 1, \"amount\": 5000}");

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void createWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/allowance-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"familyId\": 3, \"receiverId\": 1, \"amount\": 5000}")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MvcTestResult create(String body) {
		return mvc.post().uri("/api/allowance-requests")
				.header("Authorization", kid())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String kid() {
		return "Bearer " + jwtProvider.createAccessToken(2L, "USER");
	}

}
