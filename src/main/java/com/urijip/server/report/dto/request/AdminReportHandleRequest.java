package com.urijip.server.report.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminReportHandleRequest(@NotNull ReportAction action) {
}
