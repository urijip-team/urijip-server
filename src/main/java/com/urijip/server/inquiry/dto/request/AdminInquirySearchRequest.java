package com.urijip.server.inquiry.dto.request;

// GET /admin/inquiries의 쿼리 파라미터. 모두 선택이고 answered가 없으면 모두, page는 0, size는 20이 기본이다.
public record AdminInquirySearchRequest(Boolean answered, Integer page, Integer size) {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	public int pageOrDefault() {
		return page == null ? 0 : Math.max(page, 0);
	}

	public int sizeOrDefault() {
		return size == null ? DEFAULT_SIZE : Math.clamp(size, 1, MAX_SIZE);
	}

}
