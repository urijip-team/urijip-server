package com.urijip.server.inquiry.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.inquiry.entity.Inquiry;

// answer와 answeredAt은 답변 전에는 null이다.
public record InquiryResponse(Long id, String title, String content, String answer, OffsetDateTime answeredAt) {

	public static InquiryResponse from(Inquiry inquiry) {
		return new InquiryResponse(inquiry.getId(), inquiry.getTitle(), inquiry.getContent(), inquiry.getAnswer(),
				SeoulTime.toOffset(inquiry.getAnsweredAt()));
	}

}
