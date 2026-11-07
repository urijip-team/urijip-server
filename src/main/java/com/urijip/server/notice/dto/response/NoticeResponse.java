package com.urijip.server.notice.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.notice.entity.Notice;

public record NoticeResponse(Long id, String title, String content, OffsetDateTime createdAt) {

	public static NoticeResponse from(Notice notice) {
		return new NoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(),
				SeoulTime.toOffset(notice.getCreatedAt()));
	}

}
