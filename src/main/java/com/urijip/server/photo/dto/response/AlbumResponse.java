package com.urijip.server.photo.dto.response;

import com.urijip.server.photo.entity.Album;

// coverUrl은 대표 사진 썸네일의 서명 URL이다. 사진이 없으면 null.
public record AlbumResponse(Long id, String title, String coverUrl, long photoCount, Long createdBy) {

	public static AlbumResponse of(Album album, String coverUrl, long photoCount) {
		return new AlbumResponse(album.getId(), album.getTitle(), coverUrl, photoCount,
				album.getCreatedBy().getId());
	}

}
