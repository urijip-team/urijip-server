package com.urijip.server.notice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.notice.dto.request.AdminNoticeCreateRequest;
import com.urijip.server.notice.dto.request.AdminNoticeUpdateRequest;
import com.urijip.server.notice.dto.response.AdminNoticeResponse;
import com.urijip.server.notice.service.AdminNoticeService;
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

@WebMvcTest(AdminNoticeController.class)
@Import(SecurityConfig.class)
class AdminNoticeControllerTest {

	private static final AdminNoticeResponse NOTICE = new AdminNoticeResponse(3L, "서버 점검 안내", "내일 새벽 2시에 점검합니다.",
			false, new AdminNoticeResponse.CreatedBy(100L, "관리자"));

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AdminNoticeService adminNoticeService;

	@Test
	void createReturnsCreated() {
		given(adminNoticeService.create(100L, new AdminNoticeCreateRequest("서버 점검 안내", "내일 새벽 2시에 점검합니다.")))
				.willReturn(NOTICE);

		MvcTestResult result = create("""
				{"title": "서버 점검 안내", "content": "내일 새벽 2시에 점검합니다.", "push": true}
				""", admin());

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.title").isEqualTo("서버 점검 안내");
		assertThat(result).bodyJson().extractingPath("$.data.content").isEqualTo("내일 새벽 2시에 점검합니다.");
		assertThat(result).bodyJson().extractingPath("$.data.isPushed").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.data.createdBy.name").isEqualTo("관리자");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"content\": \"내용\"}",
			"{\"title\": \"제목\"}",
			"{\"title\": \" \", \"content\": \"내용\"}",
			"{\"title\": \"제목\", \"content\": \" \"}"
	})
	void createWithInvalidBodyReturnsBadRequest(String body) {
		MvcTestResult result = create(body, admin());

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(adminNoticeService).should(never()).create(any(), any());
	}

	@Test
	void createWithTooLongTitleReturnsBadRequest() {
		MvcTestResult result = create("{\"title\": \"%s\", \"content\": \"내용\"}".formatted("가".repeat(101)), admin());

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createWithTooLongContentReturnsBadRequest() {
		MvcTestResult result = create("{\"title\": \"제목\", \"content\": \"%s\"}".formatted("가".repeat(5001)), admin());

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createByUserIsForbidden() {
		MvcTestResult result = create("{\"title\": \"제목\", \"content\": \"내용\"}", user());

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		then(adminNoticeService).should(never()).create(any(), any());
	}

	@Test
	void createWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/admin/notices")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\": \"제목\", \"content\": \"내용\"}")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void updateReturnsUpdatedNotice() {
		given(adminNoticeService.update(3L, new AdminNoticeUpdateRequest(null, "새벽 3시로 바뀌었습니다.")))
				.willReturn(new AdminNoticeResponse(3L, "서버 점검 안내", "새벽 3시로 바뀌었습니다.", false, NOTICE.createdBy()));

		MvcTestResult result = update("{\"content\": \"새벽 3시로 바뀌었습니다.\"}", admin());

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.title").isEqualTo("서버 점검 안내");
		assertThat(result).bodyJson().extractingPath("$.data.content").isEqualTo("새벽 3시로 바뀌었습니다.");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"title\": \" \"}", "{\"content\": \"\"}", "{\"content\": \" \\n \"}"})
	void updateWithBlankFieldReturnsBadRequest(String body) {
		MvcTestResult result = update(body, admin());

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(adminNoticeService).should(never()).update(any(), any());
	}

	@Test
	void updateWithTooLongTitleReturnsBadRequest() {
		MvcTestResult result = update("{\"title\": \"%s\"}".formatted("가".repeat(101)), admin());

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void updateUnknownNoticeReturnsNotFound() {
		given(adminNoticeService.update(3L, new AdminNoticeUpdateRequest("제목", null)))
				.willThrow(new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

		MvcTestResult result = update("{\"title\": \"제목\"}", admin());

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("NOTICE_NOT_FOUND");
	}

	@Test
	void updateByUserIsForbidden() {
		MvcTestResult result = update("{\"title\": \"제목\"}", user());

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		then(adminNoticeService).should(never()).update(any(), any());
	}

	@Test
	void deleteReturnsOk() {
		MvcTestResult result = mvc.delete().uri("/admin/notices/3").header("Authorization", admin()).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		then(adminNoticeService).should().delete(3L);
	}

	@Test
	void deleteUnknownNoticeReturnsNotFound() {
		willThrow(new BusinessException(ErrorCode.NOTICE_NOT_FOUND)).given(adminNoticeService).delete(3L);

		MvcTestResult result = mvc.delete().uri("/admin/notices/3").header("Authorization", admin()).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("NOTICE_NOT_FOUND");
	}

	@Test
	void deleteByUserIsForbidden() {
		MvcTestResult result = mvc.delete().uri("/admin/notices/3").header("Authorization", user()).exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		then(adminNoticeService).should(never()).delete(any());
	}

	private MvcTestResult update(String body, String authorization) {
		return mvc.patch().uri("/admin/notices/3")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult create(String body, String authorization) {
		return mvc.post().uri("/admin/notices")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String admin() {
		return "Bearer " + jwtProvider.createAccessToken(100L, "ADMIN");
	}

	private String user() {
		return "Bearer " + jwtProvider.createAccessToken(1L, "USER");
	}

}
