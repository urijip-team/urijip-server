package com.urijip.server.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.report.dto.request.ReportCreateRequest;
import com.urijip.server.report.dto.response.ReportResponse;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;
import com.urijip.server.report.service.ReportService;
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

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private ReportService reportService;

	@Test
	void createReturnsCreated() {
		given(reportService.create(1L, new ReportCreateRequest(ReportTargetType.PHOTO, 42L, "부적절한 사진")))
				.willReturn(new ReportResponse(7L, ReportStatus.PENDING));

		MvcTestResult result = create("""
				{"targetType": "PHOTO", "targetId": 42, "reason": "부적절한 사진"}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.reportId").isEqualTo(7);
		assertThat(result).bodyJson().extractingPath("$.data.status").isEqualTo("PENDING");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"targetId\": 42, \"reason\": \"욕설\"}",
			"{\"targetType\": \"USER\", \"reason\": \"욕설\"}",
			"{\"targetType\": \"USER\", \"targetId\": 42}",
			"{\"targetType\": \"USER\", \"targetId\": 42, \"reason\": \" \"}"})
	void createWithMissingFieldReturnsBadRequest(String body) {
		MvcTestResult result = create(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(reportService).should(never()).create(any(), any());
	}

	@Test
	void createWithTooLongReasonReturnsBadRequest() {
		MvcTestResult result = create("""
				{"targetType": "USER", "targetId": 42, "reason": "%s"}
				""".formatted("가".repeat(256)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
	}

	@Test
	void createWithUnknownTargetTypeReturnsBadRequest() {
		MvcTestResult result = create("""
				{"targetType": "CHAT", "targetId": 42, "reason": "욕설"}
				""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		then(reportService).should(never()).create(any(), any());
	}

	@Test
	void createOnOtherFamilyPhotoReturnsForbidden() {
		given(reportService.create(any(), any())).willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = create("""
				{"targetType": "PHOTO", "targetId": 42, "reason": "부적절한 사진"}
				""");

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void createWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/reports")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"targetType": "USER", "targetId": 42, "reason": "욕설"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MvcTestResult create(String body) {
		return mvc.post().uri("/api/reports")
				.header("Authorization", bearer(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId, "USER");
	}

}
