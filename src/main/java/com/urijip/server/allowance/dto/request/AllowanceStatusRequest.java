package com.urijip.server.allowance.dto.request;

import com.urijip.server.allowance.entity.AllowanceStatus;
import jakarta.validation.constraints.NotNull;

// SENT(보냄)나 REJECTED(거절)만 받는다. REQUESTED는 INVALID_INPUT이다.
public record AllowanceStatusRequest(@NotNull AllowanceStatus status) {
}
