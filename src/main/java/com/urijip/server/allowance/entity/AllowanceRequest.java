package com.urijip.server.allowance.entity;

import java.time.LocalDateTime;

import com.urijip.server.family.entity.Family;
import com.urijip.server.global.entity.BaseTimeEntity;
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

// 처리(SENT·REJECTED)되기 전에는 completedAt이 null이다.
@Entity
@Table(name = "allowance_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AllowanceRequest extends BaseTimeEntity {

	public static final int MAX_AMOUNT = 10_000_000;

	public static final int MAX_REASON_LENGTH = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "requester_id", nullable = false)
	private User requester;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver_id", nullable = false)
	private User receiver;

	@Column(nullable = false)
	private int amount;

	@Column(length = MAX_REASON_LENGTH)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AllowanceStatus status;

	private LocalDateTime completedAt;

	public AllowanceRequest(Family family, User requester, User receiver, int amount, String reason) {
		this.family = family;
		this.requester = requester;
		this.receiver = receiver;
		this.amount = amount;
		this.reason = reason;
		this.status = AllowanceStatus.REQUESTED;
	}

}
