package com.urijip.server.sos.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.urijip.server.family.entity.Family;
import com.urijip.server.global.entity.BaseTimeEntity;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "sos_alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SosAlert extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal longitude;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SosStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resolved_by")
	private User resolvedBy;

	private LocalDateTime resolvedAt;

	public SosAlert(Family family, User user, BigDecimal latitude, BigDecimal longitude) {
		this.family = family;
		this.user = user;
		this.latitude = latitude;
		this.longitude = longitude;
		this.status = SosStatus.ACTIVE;
	}

	public boolean isResolved() {
		return status == SosStatus.RESOLVED;
	}

	public void resolve(User resolver) {
		this.status = SosStatus.RESOLVED;
		this.resolvedBy = resolver;
		this.resolvedAt = LocalDateTime.now(SeoulTime.ZONE);
	}

}
