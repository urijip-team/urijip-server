package com.urijip.server.sos.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SosSendRequest(
		@NotNull Long familyId,
		@NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {
}
