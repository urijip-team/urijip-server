package com.urijip.server.photo.entity;

import com.urijip.server.family.entity.Family;
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

@Entity
@Table(name = "albums")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@Column(nullable = false, length = 50)
	private String title;

	// 대표 사진. 처음 올린 사진이 되고, 그 사진이 지워지면 남은 사진 중 가장 최근 것으로 바뀐다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cover_photo_id")
	private Photo coverPhoto;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	@Builder
	private Album(Family family, String title, User createdBy) {
		this.family = family;
		this.title = title;
		this.createdBy = createdBy;
	}

	public void changeCoverPhoto(Photo coverPhoto) {
		this.coverPhoto = coverPhoto;
	}

}
