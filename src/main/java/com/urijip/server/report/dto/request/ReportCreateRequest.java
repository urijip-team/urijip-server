package com.urijip.server.report.dto.request;

import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
		@NotNull ReportTargetType targetType,
		@NotNull Long targetId,
		@NotBlank @Size(max = Report.MAX_REASON_LENGTH) String reason) {
}
