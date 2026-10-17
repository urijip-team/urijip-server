package com.urijip.server.photo.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.photo.entity.PhotoComment;

public record PhotoCommentResponse(Long id, String content, Author author, OffsetDateTime createdAt) {

	public record Author(Long id, String name) {
	}

	public static PhotoCommentResponse from(PhotoComment comment) {
		return new PhotoCommentResponse(comment.getId(), comment.getContent(),
				new Author(comment.getUser().getId(), comment.getUser().getName()),
				SeoulTime.toOffset(comment.getCreatedAt()));
	}

}
