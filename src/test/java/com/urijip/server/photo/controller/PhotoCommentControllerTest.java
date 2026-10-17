package com.urijip.server.photo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.photo.dto.response.PhotoCommentResponse;
import com.urijip.server.photo.service.PhotoCommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(PhotoCommentController.class)
@Import(SecurityConfig.class)
class PhotoCommentControllerTest {

	private static final PhotoCommentResponse COMMENT = new PhotoCommentResponse(7L, "바다 좋다",
			new PhotoCommentResponse.Author(1L, "김엄마"), OffsetDateTime.parse("2026-10-17T14:00:00+09:00"));

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private PhotoCommentService photoCommentService;

	@Test
	void getCommentsReturnsList() {
		given(photoCommentService.getComments(5L, 1L)).willReturn(List.of(COMMENT));

		MvcTestResult result = mvc.get().uri("/api/photos/5/comments").header("Authorization", bearer(1L))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data[0].id").isEqualTo(7);
		assertThat(result).bodyJson().extractingPath("$.data[0].content").isEqualTo("바다 좋다");
		assertThat(result).bodyJson().extractingPath("$.data[0].author.id").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data[0].author.name").isEqualTo("김엄마");
		assertThat(result).bodyJson().extractingPath("$.data[0].createdAt").isEqualTo("2026-10-17T14:00:00+09:00");
	}

	@Test
	void getCommentsOfOtherFamilyReturnsForbidden() {
		given(photoCommentService.getComments(any(), any()))
				.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = mvc.get().uri("/api/photos/5/comments").header("Authorization", bearer(9L))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void getCommentsWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/api/photos/5/comments").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId, "USER");
	}

}
