package com.urijip.server.photo.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.photo.entity.Photo;

// thumbnailUrl과 imageUrl은 일정 시간만 유효한 서명 URL이다. 만료되면 목록을 다시 불러온다.
public record PhotoResponse(Long id, String thumbnailUrl, String imageUrl, Uploader uploader,
		OffsetDateTime createdAt) {

	public record Uploader(Long id, String name) {
	}

	public static PhotoResponse of(Photo photo, String thumbnailUrl, String imageUrl) {
		return new PhotoResponse(photo.getId(), thumbnailUrl, imageUrl,
				new Uploader(photo.getUploader().getId(), photo.getUploader().getName()),
				SeoulTime.toOffset(photo.getCreatedAt()));
	}

}
