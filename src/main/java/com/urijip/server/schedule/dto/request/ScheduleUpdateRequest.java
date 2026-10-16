package com.urijip.server.schedule.dto.request;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 보낸 필드만 바꾼다. memo는 빈 문자열, participantIds는 빈 배열을 보내면 지워진다.
public record ScheduleUpdateRequest(
		@Pattern(regexp = ".*\\S.*", message = "비워 둘 수 없습니다")
		@Size(max = 50, message = "50자 이하여야 합니다") String title,
		@Size(max = 1000, message = "1000자 이하여야 합니다") String memo,
		OffsetDateTime startAt,
		OffsetDateTime endAt,
		Boolean allDay,
		List<@NotNull Long> participantIds) {
}
