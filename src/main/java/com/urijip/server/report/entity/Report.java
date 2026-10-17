package com.urijip.server.report.entity;

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

// 대상은 종류에 따라 다른 테이블이라 FK 없이 id만 둔다. 대상이 지워져도 신고 기록은 남는다.
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

	public static final int MAX_REASON_LENGTH = 255;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReportTargetType targetType;

	@Column(nullable = false)
	private Long targetId;

	@Column(nullable = false, length = MAX_REASON_LENGTH)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReportStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "handled_by")
	private User handledBy;

	public Report(User reporter, ReportTargetType targetType, Long targetId, String reason) {
		this.reporter = reporter;
		this.targetType = targetType;
		this.targetId = targetId;
		this.reason = reason;
		this.status = ReportStatus.PENDING;
	}

	public boolean isPending() {
		return status == ReportStatus.PENDING;
	}

	public void handle(User admin, ReportStatus result) {
		this.handledBy = admin;
		this.status = result;
	}

}
