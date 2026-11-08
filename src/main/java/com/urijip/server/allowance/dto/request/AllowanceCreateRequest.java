package com.urijip.server.allowance.dto.request;

import com.urijip.server.allowance.entity.AllowanceRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// reason은 선택이다. 비워 보내면 저장하지 않는다.
public record AllowanceCreateRequest(
		@NotNull Long familyId,
		@NotNull Long receiverId,
		@NotNull @Min(1) @Max(AllowanceRequest.MAX_AMOUNT) Integer amount,
		@Size(max = AllowanceRequest.MAX_REASON_LENGTH) String reason) {
}
