package com.urijip.server.schedule.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TodoCreateRequest(
		@NotBlank @Size(max = 50, message = "50자 이하여야 합니다") String title,
		Long assigneeId,
		LocalDate dueDate) {
}
