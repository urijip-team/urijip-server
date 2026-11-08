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

	public NotificationSetting(User user) {
		this.user = user;
	}

}
