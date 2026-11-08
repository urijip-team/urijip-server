package com.urijip.server.chat.dto.request;

import jakarta.validation.constraints.NotNull;

// STOMP /pub/chat/read 본문. messageId까지 읽었다.
public record ChatReadRequest(@NotNull Long familyId, @NotNull Long messageId) {
}
