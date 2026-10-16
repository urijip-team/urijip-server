package com.urijip.server.family.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.urijip.server.family.dto.request.AdminFamilyListRequest;
import com.urijip.server.family.dto.request.AdminFamilyStatusRequest;
import com.urijip.server.family.dto.response.AdminFamilyResponse;
import com.urijip.server.family.entity.FamilyStatus;
import com.urijip.server.family.service.AdminFamilyService;
import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
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

@WebMvcTest(AdminFamilyController.class)
@Import(SecurityConfig.class)
class AdminFamilyControllerTest {

	private static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2026, 10, 2, 14, 0, 0, 0,
			ZoneOffset.ofHours(9));
	private static final AdminFamilyResponse KIMS = new AdminFamilyResponse(3L, "김씨네", 4, FamilyStatus.SUSPENDED,
			CREATED_AT);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AdminFamilyService adminFamilyService;

	@Test
	void getFamiliesReturnsPage() {
		given(adminFamilyService.getFamilies(new AdminFamilyListRequest(1, 10)))
				.willReturn(new PageResponse<>(List.of(KIMS), 1, 10, 11, 2));

		MvcTestResult result = mvc.get().uri("/admin/families?page=1&size=10")
				.header("Authorization", admin())
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].name").isEqualTo("김씨네");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].memberCount").isEqualTo(4);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].status").isEqualTo("SUSPENDED");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].createdAt")
				.isEqualTo("2026-10-02T14:00:00+09:00");
		assertThat(result).bodyJson().doesNotHavePath("$.data.content[0].inviteCode");
		assertThat(result).bodyJson().extractingPath("$.data.page").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data.totalElements").isEqualTo(11);
	}

	@Test
	void getFamiliesUsesDefaults() {
		mvc.get().uri("/admin/families").header("Authorization", admin()).exchange();

		then(adminFamilyService).should().getFamilies(new AdminFamilyListRequest(null, null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"?page=abc", "?size=abc"})
	void getFamiliesWithInvalidParamReturnsBadRequest(String query) {
		MvcTestResult result = mvc.get().uri("/admin/families" + query).header("Authorization", admin()).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(adminFamilyService).should(never()).getFamilies(any());
	}

	@Test
	void getFamiliesByNonAdminReturnsForbidden() {
		MvcTestResult result = mvc.get().uri("/admin/families")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L, "USER"))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("ACCESS_DENIED");
		then(adminFamilyService).should(never()).getFamilies(any());
	}

	@Test
	void getFamiliesWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.get().uri("/admin/families").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void changeStatusReturnsFamily() {
		given(adminFamilyService.changeStatus(3L, new AdminFamilyStatusRequest(FamilyStatus.SUSPENDED)))
				.willReturn(KIMS);

		MvcTestResult result = changeStatus("""
				{"status": "SUSPENDED"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.status").isEqualTo("SUSPENDED");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"status\": \"DELETED\"}"})
	void changeStatusWithInvalidBodyReturnsBadRequest(String body) {
		MvcTestResult result = changeStatus(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(adminFamilyService).should(never()).changeStatus(any(), any());
	}

	@Test
	void changeStatusOfUnknownFamilyReturnsNotFound() {
		given(adminFamilyService.changeStatus(any(), any()))
				.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		MvcTestResult result = changeStatus("""
				{"status": "ACTIVE"}
				""");

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_FOUND");
	}

	@Test
	void changeStatusByNonAdminReturnsForbidden() {
		MvcTestResult result = mvc.patch().uri("/admin/families/3/status")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(2L, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status": "SUSPENDED"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		then(adminFamilyService).should(never()).changeStatus(any(), any());
	}

	private MvcTestResult changeStatus(String body) {
		return mvc.patch().uri("/admin/families/3/status")
				.header("Authorization", admin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String admin() {
		return "Bearer " + jwtProvider.createAccessToken(100L, "ADMIN");
	}

}
