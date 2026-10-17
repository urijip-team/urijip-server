package com.urijip.server.report.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;
import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;

// handledBy는 처리하거나 반려한 관리자이고, 처리 전에는 null이다.
public record AdminReportResponse(Long id, ReportTargetType targetType, Long targetId, String reason,
		ReportStatus status, Reporter reporter, HandledBy handledBy, OffsetDateTime createdAt) {

	public record Reporter(Long id, String email, String name) {
	}

	public record HandledBy(Long id, String name) {
	}

	public static AdminReportResponse from(Report report) {
		User reporter = report.getReporter();
		User handler = report.getHandledBy();
		return new AdminReportResponse(report.getId(), report.getTargetType(), report.getTargetId(),
				report.getReason(), report.getStatus(),
				new Reporter(reporter.getId(), reporter.getEmail(), reporter.getName()),
				handler == null ? null : new HandledBy(handler.getId(), handler.getName()),
				SeoulTime.toOffset(report.getCreatedAt()));
	}

}
