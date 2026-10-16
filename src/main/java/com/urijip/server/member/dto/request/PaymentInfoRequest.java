package com.urijip.server.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentInfoRequest(
		@NotBlank @Size(max = 20, message = "20자 이하여야 합니다") String bankName,
		@NotBlank @Pattern(regexp = "\\d{10,16}", message = "하이픈 없이 숫자 10~16자리여야 합니다") String accountNumber,
		@NotBlank @Size(max = 30, message = "30자 이하여야 합니다") String accountHolder,
		@Size(max = 255, message = "255자 이하여야 합니다")
		@Pattern(regexp = "https://(toss\\.me|qr\\.kakaopay\\.com)/\\S+",
				message = "토스(toss.me)나 카카오페이(qr.kakaopay.com) 송금 링크만 등록할 수 있습니다") String transferLink) {
}
