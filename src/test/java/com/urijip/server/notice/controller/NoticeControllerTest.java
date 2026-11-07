package com.urijip.server.notice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.notice.dto.request.NoticeSearchRequest;
import com.urijip.server.notice.dto.response.NoticeResponse;
import com.urijip.server.notice.service.NoticeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(NoticeController.class)
@Import(SecurityConfig.class)
class NoticeControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private NoticeService noticeService;

	@Test
	void getNoticesReturnsPage() {
		given(noticeService.getNotices(new NoticeSearchRequest(1, 10)))
				.willReturn(new PageResponse<>(List.of(new NoticeResponse(3L, "서버 점검 안내", "내일 새벽 2시에 점검합니다.",
						OffsetDateTime.parse("2026-11-08T10:00:00+09:00"))),
						1, 10, 11, 2));

		MvcTestResult result = mvc.get().uri("/api/notices?page=1&size=10")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L, "USER"))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].title").isEqualTo("서버 점검 안내");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].content").isEqualTo("내일 새벽 2시에 점검합니다.");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].createdAt")
				.isEqualTo("2026-11-08T10:00:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data.totalElements").isEqualTo(11);
	}

	@Test
	void getNoticesUsesDefaults() {
		given(noticeService.getNotices(new NoticeSearchRequest(null, null)))
				.willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		MvcTestResult result = mvc.get().uri("/api/notices")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L, "USER"))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.size").isEqualTo(20);
	}

	@Test
	void adminCanReadNotices() {
		given(noticeService.getNotices(new NoticeSearchRequest(null, null)))
				.willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		MvcTestResult result = mvc.get().uri("/api/notices")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(100L, "ADMIN"))
				.exchange();

		assertThat(result).hasStatusOk();
	}

	@Test
	void getNoticesWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/api/notices").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
