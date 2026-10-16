package com.urijip.server.photo.controller;

import java.util.List;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.photo.dto.request.AlbumCreateRequest;
import com.urijip.server.photo.dto.response.AlbumResponse;
import com.urijip.server.photo.service.AlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlbumController {

	private final AlbumService albumService;

	@GetMapping("/api/families/{familyId}/albums")
	public ApiResponse<List<AlbumResponse>> getAlbums(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId) {
		return ApiResponse.success(albumService.getAlbums(familyId, userId));
	}

	@PostMapping("/api/families/{familyId}/albums")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AlbumResponse> create(@AuthenticationPrincipal Long userId, @PathVariable Long familyId,
			@Valid @RequestBody AlbumCreateRequest request) {
		return ApiResponse.success(albumService.create(familyId, userId, request));
	}

}
