package com.urijip.server.notice.dto.request;

// GET /api/notices의 쿼리 파라미터. 모두 선택이고 page는 0, size는 20이 기본이다.
public record NoticeSearchRequest(Integer page, Integer size) {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	public int pageOrDefault() {
		return page == null ? 0 : Math.max(page, 0);
	}

	public int sizeOrDefault() {
		return size == null ? DEFAULT_SIZE : Math.clamp(size, 1, MAX_SIZE);
	}

}
