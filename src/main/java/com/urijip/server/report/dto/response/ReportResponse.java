package com.urijip.server.report.dto.response;

import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportStatus;

public record ReportResponse(Long reportId, ReportStatus status) {

	public static ReportResponse from(Report report) {
		return new ReportResponse(report.getId(), report.getStatus());
	}

}
