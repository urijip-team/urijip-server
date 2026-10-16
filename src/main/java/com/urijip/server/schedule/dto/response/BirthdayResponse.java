package com.urijip.server.schedule.dto.response;

import java.time.LocalDate;

public record BirthdayResponse(Long userId, String name, LocalDate date) {
}
