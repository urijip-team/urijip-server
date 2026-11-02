package com.urijip.server.inquiry.dto.request;

import com.urijip.server.inquiry.entity.Inquiry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminInquiryAnswerRequest(@NotBlank @Size(max = Inquiry.MAX_TEXT_LENGTH) String answer) {
}
