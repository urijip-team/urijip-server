package com.urijip.server.sos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.sos.dto.request.SosSearchRequest;
import com.urijip.server.sos.dto.request.SosSendRequest;
import com.urijip.server.sos.dto.response.SosCreateResponse;
import com.urijip.server.sos.dto.response.SosResponse;
import com.urijip.server.sos.entity.SosStatus;
import com.urijip.server.sos.service.SosService;
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

@WebMvcTest(SosController.class)
@Import(SecurityConfig.class)
class SosControllerTest {

	private static final SosResponse RESOLVED = new SosResponse(12L, 2L, "첫째", 35.1595, 126.8526, SosStatus.RESOLVED,
			OffsetDateTime.parse("2026-10-02T14:00:00+09:00"), new SosResponse.ResolvedBy(1L, "엄마"),
			OffsetDateTime.parse("2026-10-02T14:10:00+09:00"));

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private SosService sosService;

	@Test
	void sendReturnsCreated() {
		given(sosService.send(1L, new SosSendRequest(3L, new BigDecimal("35.1595"), new BigDecimal("126.8526"))))
				.willReturn(new SosCreateResponse(12L, OffsetDateTime.parse("2026-10-02T14:00:00+09:00")));

		MvcTestResult result = send("""
				{"familyId": 3, "latitude": 35.1595, "longitude": 126.8526}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.sosId").isEqualTo(12);
		assertThat(result).bodyJson().extractingPath("$.data.createdAt").isEqualTo("2026-10-02T14:00:00+09:00");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"latitude\": 35.1, \"longitude\": 126.8}",
			"{\"familyId\": 3, \"longitude\": 126.8}",
			"{\"familyId\": 3, \"latitude\": 35.1}",
			"{\"familyId\": 3, \"latitude\": 90.1, \"longitude\": 126.8}",
			"{\"familyId\": 3, \"latitude\": 35.1, \"longitude\": -180.1}"
	})
	void sendWithInvalidBodyReturnsBadRequest(String body) {
		MvcTestResult result = send(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(sosService).should(never()).send(any(), any());
	}

	@Test
	void sendByNonMemberReturnsForbidden() {
		given(sosService.send(any(), any())).willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = send("{\"familyId\": 3, \"latitude\": 35.1, \"longitude\": 126.8}");

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void sendWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/sos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"familyId\": 3, \"latitude\": 35.1, \"longitude\": 126.8}")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void resolveReturnsResolvedAlert() {
		given(sosService.resolve(12L, 1L)).willReturn(RESOLVED);

		MvcTestResult result = mvc.patch().uri("/api/sos/12/resolve").header("Authorization", user()).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.sosId").isEqualTo(12);
		assertThat(result).bodyJson().extractingPath("$.data.userName").isEqualTo("첫째");
		assertThat(result).bodyJson().extractingPath("$.data.latitude").isEqualTo(35.1595);
		assertThat(result).bodyJson().extractingPath("$.data.status").isEqualTo("RESOLVED");
		assertThat(result).bodyJson().extractingPath("$.data.resolvedBy.name").isEqualTo("엄마");
		assertThat(result).bodyJson().extractingPath("$.data.resolvedAt").isEqualTo("2026-10-02T14:10:00+09:00");
	}

	@Test
	void resolveAlreadyResolvedReturnsConflict() {
		given(sosService.resolve(12L, 1L)).willThrow(new BusinessException(ErrorCode.SOS_ALREADY_RESOLVED));

		MvcTestResult result = mvc.patch().uri("/api/sos/12/resolve").header("Authorization", user()).exchange();

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("SOS_ALREADY_RESOLVED");
	}

	@Test
	void resolveUnknownReturnsNotFound() {
		given(sosService.resolve(12L, 1L)).willThrow(new BusinessException(ErrorCode.SOS_NOT_FOUND));

		MvcTestResult result = mvc.patch().uri("/api/sos/12/resolve").header("Authorization", user()).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("SOS_NOT_FOUND");
	}

	@Test
	void resolveWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.patch().uri("/api/sos/12/resolve").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void getHistoryReturnsPage() {
		given(sosService.getHistory(3L, 1L, new SosSearchRequest(1, 10)))
				.willReturn(new PageResponse<>(List.of(RESOLVED), 1, 10, 11, 2));

		MvcTestResult result = mvc.get().uri("/api/families/3/sos?page=1&size=10")
				.header("Authorization", user())
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].sosId").isEqualTo(12);
		assertThat(result).bodyJson().extractingPath("$.data.totalElements").isEqualTo(11);
	}

	@Test
	void getHistoryByNonMemberReturnsForbidden() {
		given(sosService.getHistory(3L, 1L, new SosSearchRequest(null, null)))
				.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = mvc.get().uri("/api/families/3/sos").header("Authorization", user()).exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void getHistoryWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/api/families/3/sos").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MvcTestResult send(String body) {
		return mvc.post().uri("/api/sos")
				.header("Authorization", user())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String user() {
		return "Bearer " + jwtProvider.createAccessToken(1L, "USER");
	}

}
