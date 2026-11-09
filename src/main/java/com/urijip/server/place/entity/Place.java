package com.urijip.server.place.entity;

import java.math.BigDecimal;

import com.urijip.server.family.entity.Family;
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

// 가족이 함께 쓰는 장소(집, 학교 등). 멤버가 반경 안에 들어오거나 나가면 가족에게 알린다(LOC-05).
@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

	public static final int MAX_NAME_LENGTH = 30;

	public static final int MAX_PLACES_PER_FAMILY = 10;

	public static final int MIN_RADIUS_METERS = 50;

	public static final int MAX_RADIUS_METERS = 500;

	public static final int DEFAULT_RADIUS_METERS = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@Column(nullable = false, length = MAX_NAME_LENGTH)
	private String name;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal longitude;

	@Column(name = "radius_m", nullable = false)
	private int radiusMeters;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

	public Place(Family family, User createdBy, String name, BigDecimal latitude, BigDecimal longitude,
			int radiusMeters) {
		this.family = family;
		this.createdBy = createdBy;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.radiusMeters = radiusMeters;
	}

	// null인 값은 바꾸지 않는다. 위도·경도는 함께 바꾼다.
	public void update(String name, BigDecimal latitude, BigDecimal longitude, Integer radiusMeters) {
		if (name != null) {
			this.name = name;
		}
		if (latitude != null && longitude != null) {
			this.latitude = latitude;
			this.longitude = longitude;
		}
		if (radiusMeters != null) {
			this.radiusMeters = radiusMeters;
		}
	}

	public boolean belongsTo(Long familyId) {
		return family.getId().equals(familyId);
	}

}
