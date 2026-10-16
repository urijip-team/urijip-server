package com.urijip.server.schedule.dto.response;

import java.util.List;

public record MonthlyScheduleResponse(List<ScheduleResponse> schedules, List<BirthdayResponse> birthdays) {
}
