package com.urijip.server.chat.dto.response;

// STOMP /sub/family/{familyId}/read. userId가 previousLastReadMessageId(없으면 null) 다음부터 lastReadMessageId까지 읽었다.
// 앱은 그 구간에서 userId가 보내지 않은 메시지의 unreadCount를 1씩 줄인다.
public record ChatReadResponse(Long userId, Long previousLastReadMessageId, Long lastReadMessageId) {
}
