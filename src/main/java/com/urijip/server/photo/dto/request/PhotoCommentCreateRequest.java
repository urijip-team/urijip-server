package com.urijip.server.photo.dto.request;

import com.urijip.server.photo.entity.PhotoComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PhotoCommentCreateRequest(
		@NotBlank @Size(max = PhotoComment.MAX_CONTENT_LENGTH) String content) {
}
