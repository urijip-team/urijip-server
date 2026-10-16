package com.urijip.server.photo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.photo.dto.request.AlbumCreateRequest;
import com.urijip.server.photo.dto.response.AlbumResponse;
import com.urijip.server.photo.service.AlbumService;
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

@WebMvcTest(AlbumController.class)
@Import(SecurityConfig.class)
class AlbumControllerTest {

	private static final AlbumResponse ALBUM = new AlbumResponse(5L, "여름휴가", "https://s3/cover?sig", 12, 1L);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AlbumService albumService;

	@Test
	void getAlbumsReturnsList() {
		given(albumService.getAlbums(3L, 1L)).willReturn(List.of(ALBUM));

		MvcTestResult result = mvc.get().uri("/api/families/3/albums").header("Authorization", bearer(1L)).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data[0].id").isEqualTo(5);
		assertThat(result).bodyJson().extractingPath("$.data[0].title").isEqualTo("여름휴가");
		assertThat(result).bodyJson().extractingPath("$.data[0].coverUrl").isEqualTo("https://s3/cover?sig");
		assertThat(result).bodyJson().extractingPath("$.data[0].photoCount").isEqualTo(12);
		assertThat(result).bodyJson().extractingPath("$.data[0].createdBy").isEqualTo(1);
	}

	@Test
	void getAlbumsOfOtherFamilyReturnsForbidden() {
		given(albumService.getAlbums(any(), any())).willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = mvc.get().uri("/api/families/3/albums").header("Authorization", bearer(9L)).exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void getAlbumsWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/api/families/3/albums").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void createReturnsCreated() {
		given(albumService.create(3L, 1L, new AlbumCreateRequest("여름휴가"))).willReturn(ALBUM);

		MvcTestResult result = create("""
				{"title": "여름휴가"}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(5);
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"title\": \" \"}",
			"{\"title\": \"123456789012345678901234567890123456789012345678901\"}"})
	void createWithInvalidTitleReturnsBadRequest(String body) {
		MvcTestResult result = create(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(albumService).should(never()).create(any(), any(), any());
	}

	private MvcTestResult create(String body) {
		return mvc.post().uri("/api/families/3/albums")
				.header("Authorization", bearer(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId, "USER");
	}

}
