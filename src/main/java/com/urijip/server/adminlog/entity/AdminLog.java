package com.urijip.server.adminlog.entity;

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

// target_id는 target_type에 따라 다른 테이블을 가리키므로 FK를 두지 않는다. 대상이 지워져도 기록은 남는다.
@Entity
@Table(name = "admin_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLog extends BaseTimeEntity {

	public static final int MAX_DETAIL_LENGTH = 255;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_id", nullable = false)
	private User admin;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private AdminAction action;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private AdminLogTargetType targetType;

	@Column(nullable = false)
	private Long targetId;

	@Column(length = MAX_DETAIL_LENGTH)
	private String detail;

	// detail이 길면 잘라서 저장한다.
	public AdminLog(User admin, AdminAction action, AdminLogTargetType targetType, Long targetId, String detail) {
		this.admin = admin;
		this.action = action;
		this.targetType = targetType;
		this.targetId = targetId;
		this.detail = detail == null || detail.length() <= MAX_DETAIL_LENGTH
				? detail
				: detail.substring(0, MAX_DETAIL_LENGTH);
	}

}
