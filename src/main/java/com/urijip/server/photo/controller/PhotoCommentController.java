package com.urijip.server.photo.controller;

import java.util.List;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.photo.dto.response.PhotoCommentResponse;
import com.urijip.server.photo.service.PhotoCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

}
