package com.urijip.server.family.dto.request;

import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(
		@Size(max = 20, message = "20자 이하여야 합니다") String nickname) {
}
