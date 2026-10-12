package com.urijip.server.member.dto.request;

import com.urijip.server.member.entity.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceTokenRequest(
		@NotBlank @Size(max = 512) String fcmToken,
		@NotNull Platform platform) {
}
