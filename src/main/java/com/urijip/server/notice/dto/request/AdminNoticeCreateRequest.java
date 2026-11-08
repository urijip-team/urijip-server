package com.urijip.server.notice.dto.request;

import com.urijip.server.notice.entity.Notice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// push가 true면 활동 중인 모든 회원에게 푸시한다. 빠지면 보내지 않는다.
public record AdminNoticeCreateRequest(
		@NotBlank @Size(max = Notice.MAX_TITLE_LENGTH) String title,
		@NotBlank @Size(max = Notice.MAX_CONTENT_LENGTH) String content,
		Boolean push) {

	public boolean pushRequested() {
		return Boolean.TRUE.equals(push);
	}

}
