package com.urijip.server.family.dto.response;

// 읽음 처리로 마지막 읽은 메시지가 previous(없으면 null)에서 current로 바뀌었다.
public record ReadProgress(Long previousLastReadMessageId, Long lastReadMessageId) {
}
