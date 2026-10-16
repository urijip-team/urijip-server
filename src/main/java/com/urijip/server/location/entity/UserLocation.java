package com.urijip.server.location.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

// 이동 기록은 남기지 않고 회원마다 마지막 위치 한 행만 둔다. updated_at은 위치를 마지막으로 받은 시각이다.
@Entity
@Table(name = "user_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLocation {

	@Id
	private Long userId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(precision = 10, scale = 7)
	private BigDecimal longitude;

	@Column(nullable = false)
	private boolean sharingEnabled;

	private LocalDateTime updatedAt;

	public UserLocation(User user) {
		this.user = user;
		this.sharingEnabled = true;
	}

	public void updateLocation(BigDecimal latitude, BigDecimal longitude, LocalDateTime updatedAt) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.updatedAt = updatedAt;
	}

	// 공유를 끄면 저장해 둔 위치도 지운다. 다시 켜면 새 위치를 받을 때까지 위치가 없다.
	public void changeSharing(boolean sharingEnabled) {
		this.sharingEnabled = sharingEnabled;
		if (!sharingEnabled) {
			updateLocation(null, null, null);
		}
	}

}
