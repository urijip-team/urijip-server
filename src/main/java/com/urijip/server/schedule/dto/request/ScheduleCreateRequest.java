package com.urijip.server.schedule.dto.request;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// allDay를 보내지 않으면 false다.
public record ScheduleCreateRequest(
		@NotBlank @Size(max = 50, message = "50자 이하여야 합니다") String title,
		@Size(max = 1000, message = "1000자 이하여야 합니다") String memo,
		@NotNull OffsetDateTime startAt,
		@NotNull OffsetDateTime endAt,
		Boolean allDay,
		List<@NotNull Long> participantIds) {

	@AssertTrue(message = "종료 시간은 시작 시간보다 빠를 수 없습니다")
	public boolean isValidPeriod() {
		return startAt == null || endAt == null || !endAt.isBefore(startAt);
	}

}
