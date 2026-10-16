package com.urijip.server.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.member.dto.request.AdminUserSearchRequest;
import com.urijip.server.member.dto.request.AdminUserStatusRequest;
import com.urijip.server.member.dto.response.AdminUserDetailResponse;
import com.urijip.server.member.dto.response.AdminUserResponse;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.service.AdminUserService;
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

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerTest {

	private static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2026, 10, 2, 14, 0, 0, 0,
			ZoneOffset.ofHours(9));
	private static final AdminUserDetailResponse DETAIL = new AdminUserDetailResponse(1L, "mom@example.com", "김엄마",
			null, LocalDate.of(1980, 5, 5), "오늘도 화이팅", UserStatus.SUSPENDED, CREATED_AT,
			new AdminUserDetailResponse.Family(3L, "김씨네"));

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AdminUserService adminUserService;

	@Test
	void searchReturnsPage() {
		List<AdminUserResponse> users = List.of(new AdminUserResponse(1L, "mom@example.com", "김엄마",
				UserStatus.ACTIVE, CREATED_AT));
		given(adminUserService.search(new AdminUserSearchRequest("김", UserStatus.ACTIVE, 1, 10)))
				.willReturn(new PageResponse<>(users, 1, 10, 11, 2));

		MvcTestResult result = mvc.get().uri("/admin/users?keyword=김&status=ACTIVE&page=1&size=10")
				.header("Authorization", admin())
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].id").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].email").isEqualTo("mom@example.com");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].status").isEqualTo("ACTIVE");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].createdAt")
				.isEqualTo("2026-10-02T14:00:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data.page").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data.size").isEqualTo(10);
		assertThat(result).bodyJson().extractingPath("$.data.totalElements").isEqualTo(11);
		assertThat(result).bodyJson().extractingPath("$.data.totalPages").isEqualTo(2);
	}

	@Test
	void searchUsesDefaults() {
		mvc.get().uri("/admin/users").header("Authorization", admin()).exchange();

		then(adminUserService).should().search(new AdminUserSearchRequest(null, null, null, null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"?status=DELETED", "?page=abc", "?size=abc"})
	void searchWithInvalidParamReturnsBadRequest(String query) {
		MvcTestResult result = mvc.get().uri("/admin/users" + query).header("Authorization", admin()).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(adminUserService).should(never()).search(any());
	}

	@Test
	void searchByNonAdminReturnsForbidden() {
		MvcTestResult result = mvc.get().uri("/admin/users")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L, "USER"))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("ACCESS_DENIED");
		then(adminUserService).should(never()).search(any());
	}

	@Test
	void searchWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.get().uri("/admin/users").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void getUserReturnsDetail() {
		given(adminUserService.getUser(1L)).willReturn(DETAIL);

		MvcTestResult result = mvc.get().uri("/admin/users/1").header("Authorization", admin()).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.email").isEqualTo("mom@example.com");
		assertThat(result).bodyJson().extractingPath("$.data.birthday").isEqualTo("1980-05-05");
		assertThat(result).bodyJson().extractingPath("$.data.status").isEqualTo("SUSPENDED");
		assertThat(result).bodyJson().extractingPath("$.data.family.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.family.name").isEqualTo("김씨네");
		assertThat(result).bodyJson().doesNotHavePath("$.data.password");
	}

	@Test
	void getUnknownUserReturnsNotFound() {
		given(adminUserService.getUser(any())).willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

		MvcTestResult result = mvc.get().uri("/admin/users/9").header("Authorization", admin()).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("USER_NOT_FOUND");
	}

	@Test
	void changeStatusReturnsDetail() {
		given(adminUserService.changeStatus(1L, new AdminUserStatusRequest(UserStatus.SUSPENDED))).willReturn(DETAIL);

		MvcTestResult result = changeStatus("""
				{"status": "SUSPENDED"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.status").isEqualTo("SUSPENDED");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"status\": \"DELETED\"}"})
	void changeStatusWithInvalidBodyReturnsBadRequest(String body) {
		MvcTestResult result = changeStatus(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(adminUserService).should(never()).changeStatus(any(), any());
	}

	@Test
	void changeStatusOfWithdrawnUserReturnsConflict() {
		given(adminUserService.changeStatus(any(), any())).willThrow(new BusinessException(ErrorCode.USER_WITHDRAWN));

		MvcTestResult result = changeStatus("""
				{"status": "ACTIVE"}
				""");

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("USER_WITHDRAWN");
	}

	@Test
	void changeStatusByNonAdminReturnsForbidden() {
		MvcTestResult result = mvc.patch().uri("/admin/users/1/status")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(2L, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "SUSPENDED"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		then(adminUserService).should(never()).changeStatus(any(), any());
	}

	private MvcTestResult changeStatus(String body) {
		return mvc.patch().uri("/admin/users/1/status")
				.header("Authorization", admin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String admin() {
		return "Bearer " + jwtProvider.createAccessToken(100L, "ADMIN");
	}

}
