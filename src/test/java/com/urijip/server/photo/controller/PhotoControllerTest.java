package com.urijip.server.photo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.photo.TestImages;
import com.urijip.server.photo.dto.request.PhotoPageRequest;
import com.urijip.server.photo.dto.response.PhotoResponse;
import com.urijip.server.photo.service.PhotoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(PhotoController.class)
@Import(SecurityConfig.class)
class PhotoControllerTest {

	private static final PhotoResponse PHOTO = new PhotoResponse(7L, "https://s3/thumb?sig", "https://s3/image?sig",
			new PhotoResponse.Uploader(2L, "김아빠"),
			OffsetDateTime.of(2026, 10, 2, 14, 0, 0, 0, ZoneOffset.ofHours(9)));

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private PhotoService photoService;

	@Test
	void getPhotosReturnsPage() {
		given(photoService.getPhotos(5L, 1L, new PhotoPageRequest(1, 10)))
				.willReturn(new PageResponse<>(List.of(PHOTO), 1, 10, 11, 2));

		MvcTestResult result = mvc.get().uri("/api/albums/5/photos?page=1&size=10")
				.header("Authorization", bearer(1L))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].id").isEqualTo(7);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].thumbnailUrl")
				.isEqualTo("https://s3/thumb?sig");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].imageUrl").isEqualTo("https://s3/image?sig");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].uploader.id").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].uploader.name").isEqualTo("김아빠");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].createdAt")
				.isEqualTo("2026-10-02T14:00:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data.totalElements").isEqualTo(11);
	}

	@Test
	void getPhotosUsesDefaults() {
		mvc.get().uri("/api/albums/5/photos").header("Authorization", bearer(1L)).exchange();

		then(photoService).should().getPhotos(5L, 1L, new PhotoPageRequest(null, null));
	}

	@Test
	void getPhotosOfUnknownAlbumReturnsNotFound() {
		given(photoService.getPhotos(any(), any(), any())).willThrow(new BusinessException(ErrorCode.ALBUM_NOT_FOUND));

		MvcTestResult result = mvc.get().uri("/api/albums/9/photos").header("Authorization", bearer(1L)).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("ALBUM_NOT_FOUND");
	}

	@Test
	@SuppressWarnings("unchecked")
	void uploadPassesAllFilesAndReturnsCreated() {
		given(photoService.upload(eq(5L), eq(2L), anyList())).willReturn(List.of(PHOTO));

		MvcTestResult result = mvc.post().uri("/api/albums/5/photos").multipart()
				.file(TestImages.jpeg(10, 10))
				.file(TestImages.jpeg(20, 20))
				.header("Authorization", bearer(2L))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data[0].id").isEqualTo(7);
		ArgumentCaptor<List<MultipartFile>> files = ArgumentCaptor.forClass(List.class);
		then(photoService).should().upload(eq(5L), eq(2L), files.capture());
		assertThat(files.getValue()).hasSize(2);
	}

	@Test
	void uploadWithoutFilesPartReturnsBadRequest() {
		MvcTestResult result = mvc.post().uri("/api/albums/5/photos").multipart()
				.param("title", "x")
				.header("Authorization", bearer(2L))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(photoService).should(never()).upload(any(), any(), any());
	}

	@Test
	void uploadInvalidImageReturnsBadRequest() {
		given(photoService.upload(any(), any(), any())).willThrow(new BusinessException(ErrorCode.INVALID_IMAGE));

		MvcTestResult result = mvc.post().uri("/api/albums/5/photos").multipart()
				.file(TestImages.file("text".getBytes()))
				.header("Authorization", bearer(2L))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_IMAGE");
	}

	@Test
	void uploadWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/albums/5/photos").multipart().file(TestImages.jpeg(10, 10)).exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void deleteReturnsOk() {
		MvcTestResult result = mvc.delete().uri("/api/photos/7").header("Authorization", bearer(2L)).exchange();

		assertThat(result).hasStatusOk();
		then(photoService).should().delete(7L, 2L);
	}

	@Test
	void deleteByOtherMemberReturnsForbidden() {
		willThrow(new BusinessException(ErrorCode.NOT_PHOTO_UPLOADER)).given(photoService).delete(7L, 1L);

		MvcTestResult result = mvc.delete().uri("/api/photos/7").header("Authorization", bearer(1L)).exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("NOT_PHOTO_UPLOADER");
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId, "USER");
	}

}
