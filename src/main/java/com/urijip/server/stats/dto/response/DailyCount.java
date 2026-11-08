package com.urijip.server.stats.dto.response;

import java.time.LocalDate;

public record DailyCount(LocalDate date, long count) {
}
