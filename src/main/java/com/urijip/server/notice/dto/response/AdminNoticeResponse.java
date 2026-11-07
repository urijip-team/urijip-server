package com.urijip.server.notice.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;
import com.urijip.server.notice.entity.Notice;

public record AdminNoticeResponse(Long id, String title, String content, boolean isPushed, CreatedBy createdBy,
		OffsetDateTime createdAt) {

	public record CreatedBy(Long id, String name) {
	}

	public static AdminNoticeResponse from(Notice notice) {
		User admin = notice.getCreatedBy();
		return new AdminNoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(), notice.isPushed(),
				new CreatedBy(admin.getId(), admin.getName()), SeoulTime.toOffset(notice.getCreatedAt()));
	}

}
