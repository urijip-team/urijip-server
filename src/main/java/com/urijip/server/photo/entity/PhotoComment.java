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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "photo_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoComment extends BaseTimeEntity {

	public static final int MAX_CONTENT_LENGTH = 300;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "photo_id", nullable = false)
	private Photo photo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = MAX_CONTENT_LENGTH)
	private String content;

	public PhotoComment(Photo photo, User user, String content) {
		this.photo = photo;
		this.user = user;
		this.content = content;
	}

}
