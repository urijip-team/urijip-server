package com.urijip.server.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// STOMP /pub/chat 본문
public record ChatSendRequest(
		@NotNull Long familyId,
		@NotBlank @Size(max = 1000) String content) {
}
