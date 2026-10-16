package com.urijip.server.schedule.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.schedule.entity.Schedule;

public record ScheduleResponse(Long id, String title, String memo, OffsetDateTime startAt, OffsetDateTime endAt,
		boolean allDay, List<Long> participantIds, Long createdBy) {

	public static ScheduleResponse from(Schedule schedule) {
		return new ScheduleResponse(schedule.getId(), schedule.getTitle(), schedule.getMemo(),
				SeoulTime.toOffset(schedule.getStartAt()), SeoulTime.toOffset(schedule.getEndAt()),
				schedule.isAllDay(), schedule.getParticipantIds().stream().sorted().toList(),
				schedule.getCreatedBy().getId());
	}

}
