package com.urijip.server.photo.entity;

import com.urijip.server.global.entity.BaseTimeEntity;
import com.urijip.server.member.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 버킷이 비공개라 image_url, thumbnail_url 컬럼에는 URL이 아닌 S3 key를 저장한다. 응답할 때 서명 URL로 바꾼다.
@Entity
@Table(name = "photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "album_id", nullable = false)
	private Album album;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploader_id", nullable = false)
	private User uploader;

	@Column(name = "image_url", nullable = false)
	private String imageKey;

	@Column(name = "thumbnail_url", nullable = false)
	private String thumbnailKey;

	@Builder
	private Photo(Album album, User uploader, String imageKey, String thumbnailKey) {
		this.album = album;
		this.uploader = uploader;
		this.imageKey = imageKey;
		this.thumbnailKey = thumbnailKey;
	}

}
