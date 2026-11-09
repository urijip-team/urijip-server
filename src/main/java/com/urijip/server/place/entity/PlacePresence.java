package com.urijip.server.place.entity;

import com.urijip.server.global.entity.BaseTimeEntity;
import com.urijip.server.member.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원이 지금 장소 안에 있다는 기록. 도착하면 만들고 출발하면 지운다. created_at이 도착 시각이다.
// 앞 위치 하나만 보면 경계에서 GPS가 흔들릴 때 알림이 되풀이되므로 상태를 남긴다.
@Entity
@Table(name = "place_presences", uniqueConstraints = @UniqueConstraint(columnNames = {"place_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlacePresence extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	public PlacePresence(Place place, User user) {
		this.place = place;
		this.user = user;
	}

}
