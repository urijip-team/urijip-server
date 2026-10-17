package com.urijip.server.chat.dto.request;

// GET /api/families/{familyId}/messages의 쿼리 파라미터. cursor가 없으면 가장 최근 메시지부터, size는 30이 기본이다.
public record ChatMessagePageRequest(Long cursor, Integer size) {

	private static final int DEFAULT_SIZE = 30;
	private static final int MAX_SIZE = 100;

	public int sizeOrDefault() {
		return size == null ? DEFAULT_SIZE : Math.clamp(size, 1, MAX_SIZE);
	}

}
