package com.urijip.server.inquiry.dto.request;

import com.urijip.server.inquiry.entity.Inquiry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
		@NotBlank @Size(max = Inquiry.MAX_TITLE_LENGTH) String title,
		@NotBlank @Size(max = Inquiry.MAX_TEXT_LENGTH) String content) {
}
