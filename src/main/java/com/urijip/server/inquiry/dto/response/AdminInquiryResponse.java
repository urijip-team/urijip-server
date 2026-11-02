package com.urijip.server.inquiry.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.inquiry.entity.Inquiry;
import com.urijip.server.member.entity.User;

// answer, answeredBy, answeredAt은 답변 전에는 null이다.
public record AdminInquiryResponse(Long id, Author user, String title, String content, String answer,
		AnsweredBy answeredBy, OffsetDateTime answeredAt) {

	public record Author(Long id, String email, String name) {
	}

	public record AnsweredBy(Long id, String name) {
	}

	public static AdminInquiryResponse from(Inquiry inquiry) {
		User user = inquiry.getUser();
		User admin = inquiry.getAnsweredBy();
		return new AdminInquiryResponse(inquiry.getId(), new Author(user.getId(), user.getEmail(), user.getName()),
				inquiry.getTitle(), inquiry.getContent(), inquiry.getAnswer(),
				admin == null ? null : new AnsweredBy(admin.getId(), admin.getName()),
				SeoulTime.toOffset(inquiry.getAnsweredAt()));
	}

}
