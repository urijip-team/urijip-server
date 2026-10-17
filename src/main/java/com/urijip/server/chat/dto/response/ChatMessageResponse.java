package com.urijip.server.chat.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.chat.entity.ChatMessage;
import com.urijip.server.chat.entity.MessageType;
import com.urijip.server.global.time.SeoulTime;

// 이전 메시지 조회와 STOMP /sub/family/{familyId}/chat 이 같은 형식을 쓴다. 시스템 메시지는 senderId, senderName이 null이다.
public record ChatMessageResponse(Long messageId, MessageType type, Long senderId, String senderName, String content,
		OffsetDateTime createdAt) {

	public static ChatMessageResponse of(ChatMessage message, String senderName) {
		Long senderId = message.getSender() == null ? null : message.getSender().getId();
		return new ChatMessageResponse(message.getId(), message.getType(), senderId, senderName,
				message.getContent(), SeoulTime.toOffset(message.getCreatedAt()));
	}

}
