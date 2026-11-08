package com.urijip.server.stats.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.Map;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.stats.dto.request.AdminStatsRequest;
import com.urijip.server.stats.dto.response.AdminStatsResponse;
import com.urijip.server.stats.service.AdminStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(AdminStatsController.class)
@Import(SecurityConfig.class)
class AdminStatsControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AdminStatsService adminStatsService;

	@Test
	void getStatsReturnsCardsAndDailySignups() {
		given(adminStatsService.getStats(new AdminStatsRequest(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2))))
				.willReturn(AdminStatsResponse.of(120, 40, 3, 5, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2),
						Map.of(LocalDate.of(2026, 11, 2), 4L)));

		MvcTestResult result = mvc.get().uri("/admin/stats?from=2026-11-01&to=2026-11-02")
				.header("Authorization", admin())
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.totalUsers").isEqualTo(120);
		assertThat(result).bodyJson().extractingPath("$.data.totalFamilies").isEqualTo(40);
		assertThat(result).bodyJson().extractingPath("$.data.pendingReports").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.unansweredInquiries").isEqualTo(5);
		assertThat(result).bodyJson().extractingPath("$.data.from").isEqualTo("2026-11-01");
		assertThat(result).bodyJson().extractingPath("$.data.to").isEqualTo("2026-11-02");
		assertThat(result).bodyJson().extractingPath("$.data.signupCount").isEqualTo(4);
		assertThat(result).bodyJson().extractingPath("$.data.signups[0].date").isEqualTo("2026-11-01");
		assertThat(result).bodyJson().extractingPath("$.data.signups[0].count").isEqualTo(0);
		assertThat(result).bodyJson().extractingPath("$.data.signups[1].count").isEqualTo(4);
	}

	@Test
	void getStatsWithoutParamsUsesDefaults() {
		given(adminStatsService.getStats(new AdminStatsRequest(null, null)))
				.willReturn(AdminStatsResponse.of(0, 0, 0, 0, LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 8),
						Map.of()));

		assertThat(mvc.get().uri("/admin/stats").header("Authorization", admin()).exchange()).hasStatusOk();
	}

	@Test
	void getStatsWithMalformedDateReturnsBadRequest() {
		MvcTestResult result = mvc.get().uri("/admin/stats?from=2026/11/01").header("Authorization", admin()).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(adminStatsService).should(never()).getStats(any());
	}

	@Test
	void getStatsWithInvalidPeriodReturnsBadRequest() {
		given(adminStatsService.getStats(new AdminStatsRequest(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1))))
				.willThrow(new BusinessException(ErrorCode.INVALID_INPUT));

		MvcTestResult result = mvc.get().uri("/admin/stats?from=2026-11-05&to=2026-11-01")
				.header("Authorization", admin())
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
	}

	@Test
	void getStatsByUserIsForbidden() {
		MvcTestResult result = mvc.get().uri("/admin/stats")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L, "USER"))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void getStatsWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/admin/stats").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private String admin() {
		return "Bearer " + jwtProvider.createAccessToken(100L, "ADMIN");
	}

}
