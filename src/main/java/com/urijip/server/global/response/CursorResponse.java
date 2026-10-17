package com.urijip.server.global.response;

import java.util.List;

// 커서 기반 목록 응답. 다음 요청에 nextCursor를 cursor로 보낸다. 더 없으면 hasNext false, nextCursor null이다.
public record CursorResponse<T>(List<T> content, Long nextCursor, boolean hasNext) {
}
