package com.urijip.server.notice.dto.request;

import com.urijip.server.notice.entity.Notice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 명세서의 push 필드는 푸시 발송(FCM)이 생길 때 받는다. 지금 보내면 무시된다.
public record AdminNoticeCreateRequest(
		@NotBlank @Size(max = Notice.MAX_TITLE_LENGTH) String title,
		@NotBlank @Size(max = Notice.MAX_CONTENT_LENGTH) String content) {
}
