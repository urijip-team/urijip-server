package com.urijip.server.notice.dto.request;

import com.urijip.server.notice.entity.Notice;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 보낸 필드만 바꾼다. 보낸 값은 비워 둘 수 없다.
public record AdminNoticeUpdateRequest(
		@Pattern(regexp = ".*\\S.*", message = "비워 둘 수 없습니다")
		@Size(max = Notice.MAX_TITLE_LENGTH) String title,
		@Pattern(regexp = "(?s).*\\S.*", message = "비워 둘 수 없습니다")
		@Size(max = Notice.MAX_CONTENT_LENGTH) String content) {
}
