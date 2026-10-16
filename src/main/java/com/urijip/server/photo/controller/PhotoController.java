package com.urijip.server.photo.controller;

import java.util.List;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.photo.dto.request.PhotoPageRequest;
import com.urijip.server.photo.dto.response.PhotoResponse;
import com.urijip.server.photo.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PhotoController {

	private final PhotoService photoService;

	@GetMapping("/api/albums/{albumId}/photos")
	public ApiResponse<PageResponse<PhotoResponse>> getPhotos(@AuthenticationPrincipal Long userId,
			@PathVariable Long albumId, @ModelAttribute PhotoPageRequest request) {
		return ApiResponse.success(photoService.getPhotos(albumId, userId, request));
	}

	// multipart의 files 파트에 사진을 한 장 이상(최대 10장) 담아 보낸다.
	@PostMapping(path = "/api/albums/{albumId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<List<PhotoResponse>> upload(@AuthenticationPrincipal Long userId, @PathVariable Long albumId,
			@RequestPart("files") List<MultipartFile> files) {
		return ApiResponse.success(photoService.upload(albumId, userId, files));
	}

	@DeleteMapping("/api/photos/{photoId}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long photoId) {
		photoService.delete(photoId, userId);
		return ApiResponse.success(null);
	}

}
