package com.urijip.server.member.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
		@Pattern(regexp = ".*\\S.*", message = "비워 둘 수 없습니다")
		@Size(max = 50, message = "50자 이하여야 합니다") String name,
		@PastOrPresent LocalDate birthday,
		@Size(max = 100, message = "100자 이하여야 합니다") String statusMessage) {
}
