package com.urijip.server.adminlog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.adminlog.dto.request.AdminLogSearchRequest;
import com.urijip.server.adminlog.dto.response.AdminLogResponse;
import com.urijip.server.adminlog.entity.AdminAction;
import com.urijip.server.adminlog.entity.AdminLogTargetType;
import com.urijip.server.adminlog.service.AdminLogService;
import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.global.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(AdminLogController.class)
@Import(SecurityConfig.class)
class AdminLogControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AdminLogService adminLogService;

	@Test
	void searchReturnsPage() {
		given(adminLogService.search(new AdminLogSearchRequest(100L, 1, 10))).willReturn(new PageResponse<>(
				List.of(new AdminLogResponse(5L, new AdminLogResponse.Admin(100L, "관리자"),
						AdminAction.CHANGE_USER_STATUS, AdminLogTargetType.USER, 7L, "ACTIVE -> SUSPENDED",
						OffsetDateTime.parse("2026-11-08T10:00:00+09:00"))), 1, 10, 11, 2));

		MvcTestResult result = mvc.get().uri("/admin/logs?adminId=100&page=1&size=10")
				.header("Authorization", admin())
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].id").isEqualTo(5);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].admin.name").isEqualTo("관리자");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].action").isEqualTo("CHANGE_USER_STATUS");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].targetType").isEqualTo("USER");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].targetId").isEqualTo(7);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].detail").isEqualTo("ACTIVE -> SUSPENDED");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].createdAt")
				.isEqualTo("2026-11-08T10:00:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data.totalElements").isEqualTo(11);
	}

	@Test
	void searchUsesDefaults() {
		given(adminLogService.search(new AdminLogSearchRequest(null, null, null)))
				.willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		assertThat(mvc.get().uri("/admin/logs").header("Authorization", admin()).exchange()).hasStatusOk();
	}

	@Test
	void searchWithInvalidAdminIdReturnsBadRequest() {
		MvcTestResult result = mvc.get().uri("/admin/logs?adminId=abc").header("Authorization", admin()).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(adminLogService).should(never()).search(any());
	}

	@Test
	void searchByUserIsForbidden() {
		MvcTestResult result = mvc.get().uri("/admin/logs")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L, "USER"))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		then(adminLogService).should(never()).search(any());
	}

	@Test
	void searchWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/admin/logs").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private String admin() {
		return "Bearer " + jwtProvider.createAccessToken(100L, "ADMIN");
	}

}
