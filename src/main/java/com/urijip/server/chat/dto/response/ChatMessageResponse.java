package com.urijip.server.chat.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.chat.entity.ChatMessage;
import com.urijip.server.chat.entity.MessageType;
import com.urijip.server.global.time.SeoulTime;

// 이전 메시지 조회와 STOMP /sub/family/{familyId}/chat 이 같은 형식을 쓴다. 시스템 메시지는 senderId, senderName이 null이다.
// unreadCount는 보낸 사람을 뺀, 아직 이 메시지까지 읽지 않은 가족 수다.
public record ChatMessageResponse(Long messageId, MessageType type, Long senderId, String senderName, String content,
		OffsetDateTime createdAt, int unreadCount) {

	// content는 글 메시지면 내용, 사진 메시지면 사진의 서명 URL이다.
	public static ChatMessageResponse of(ChatMessage message, String senderName, String content, int unreadCount) {
		Long senderId = message.getSender() == null ? null : message.getSender().getId();
		return new ChatMessageResponse(message.getId(), message.getType(), senderId, senderName, content,
				SeoulTime.toOffset(message.getCreatedAt()), unreadCount);
	}

}
