package com.urijip.server.chat.service;

import com.urijip.server.chat.dto.response.ChatReadResponse;

public record ChatReadEvent(Long familyId, ChatReadResponse message) {
}
