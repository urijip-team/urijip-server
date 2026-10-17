package com.urijip.server.photo.controller;

import java.util.List;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.photo.dto.request.PhotoCommentCreateRequest;
import com.urijip.server.photo.dto.response.PhotoCommentResponse;
import com.urijip.server.photo.service.PhotoCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PhotoCommentController {

	private final PhotoCommentService photoCommentService;

	@GetMapping("/api/photos/{photoId}/comments")
	public ApiResponse<List<PhotoCommentResponse>> getComments(@AuthenticationPrincipal Long userId,
			@PathVariable Long photoId) {
		return ApiResponse.success(photoCommentService.getComments(photoId, userId));
	}

	@PostMapping("/api/photos/{photoId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PhotoCommentResponse> create(@AuthenticationPrincipal Long userId, @PathVariable Long photoId,
			@Valid @RequestBody PhotoCommentCreateRequest request) {
		return ApiResponse.success(photoCommentService.create(photoId, userId, request));
	}

	@DeleteMapping("/api/comments/{commentId}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long commentId) {
		photoCommentService.delete(commentId, userId);
		return ApiResponse.success(null);
	}

}
