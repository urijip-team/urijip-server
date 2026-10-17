package com.urijip.server.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.photo.dto.response.PhotoResponse;
import com.urijip.server.report.dto.request.AdminReportSearchRequest;
import com.urijip.server.report.dto.response.AdminReportResponse;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;
import com.urijip.server.report.service.AdminReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(AdminReportController.class)
@Import(SecurityConfig.class)
class AdminReportControllerTest {

	private static final AdminReportResponse REPORT = new AdminReportResponse(7L, ReportTargetType.PHOTO, 42L, null,
			new PhotoResponse(42L, "https://s3/thumb?sig", "https://s3/image?sig",
					new PhotoResponse.Uploader(3L, "악플러"), OffsetDateTime.parse("2026-10-16T09:00:00+09:00")),
			"부적절한 사진", ReportStatus.PENDING, new AdminReportResponse.Reporter(1L, "mom@example.com", "김엄마"),
			null, OffsetDateTime.parse("2026-10-17T14:00:00+09:00"));

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AdminReportService adminReportService;

	@Test
	void searchReturnsPage() {
		given(adminReportService.search(new AdminReportSearchRequest(ReportStatus.PENDING, 1, 10)))
				.willReturn(new PageResponse<>(List.of(REPORT), 1, 10, 11, 2));

		MvcTestResult result = mvc.get().uri("/admin/reports?status=PENDING&page=1&size=10")
				.header("Authorization", admin())
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].id").isEqualTo(7);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].targetType").isEqualTo("PHOTO");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].targetId").isEqualTo(42);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].targetUser").isNull();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].targetPhoto.imageUrl")
				.isEqualTo("https://s3/image?sig");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].targetPhoto.uploader.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].reason").isEqualTo("부적절한 사진");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].status").isEqualTo("PENDING");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].reporter.email")
				.isEqualTo("mom@example.com");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].handledBy").isNull();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].createdAt")
				.isEqualTo("2026-10-17T14:00:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data.totalElements").isEqualTo(11);
	}

	@Test
	void searchUsesDefaults() {
		given(adminReportService.search(new AdminReportSearchRequest(null, null, null)))
				.willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		MvcTestResult result = mvc.get().uri("/admin/reports").header("Authorization", admin()).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.size").isEqualTo(20);
	}

	@Test
	void searchWithUnknownStatusReturnsBadRequest() {
		MvcTestResult result = mvc.get().uri("/admin/reports?status=WAITING").header("Authorization", admin())
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void searchByUserIsForbidden() {
		MvcTestResult result = mvc.get().uri("/admin/reports")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L, "USER"))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void searchWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/admin/reports").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private String admin() {
		return "Bearer " + jwtProvider.createAccessToken(100L, "ADMIN");
	}

}
