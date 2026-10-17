package com.urijip.server.chat.service;

import com.urijip.server.chat.dto.response.ChatMessageResponse;

public record ChatMessageSentEvent(Long familyId, ChatMessageResponse message) {
}
