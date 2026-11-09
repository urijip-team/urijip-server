package com.urijip.server.notification.entity;

import com.urijip.server.member.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원별 푸시 종류 켜기·끄기. 행이 없으면 모두 켜진 것으로 본다.
@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

	@Id
	private Long userId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false)
	private boolean chat = true;

	@Column(nullable = false)
	private boolean schedule = true;

	@Column(nullable = false)
	private boolean allowance = true;

	@Column(nullable = false)
	private boolean notice = true;

	// 가족 장소 도착·출발 알림(LOC-05). ERD에 없어 추가했다.
	@Column(nullable = false)
	private boolean place = true;

	public NotificationSetting(User user) {
		this.user = user;
	}

	// null인 값은 바꾸지 않는다.
	public void update(Boolean chat, Boolean schedule, Boolean allowance, Boolean notice, Boolean place) {
		if (chat != null) {
			this.chat = chat;
		}
		if (schedule != null) {
			this.schedule = schedule;
		}
		if (allowance != null) {
			this.allowance = allowance;
		}
		if (notice != null) {
			this.notice = notice;
		}
		if (place != null) {
			this.place = place;
		}
	}

}
