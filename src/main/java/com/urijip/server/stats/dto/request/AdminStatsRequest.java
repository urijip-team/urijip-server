package com.urijip.server.stats.dto.request;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import org.springframework.format.annotation.DateTimeFormat;

// GET /admin/stats의 쿼리 파라미터(2026-11-01 형식). 둘 다 선택이다.
// to가 없으면 오늘, from이 없으면 to를 포함한 7일 전부터다. from이 to보다 늦거나 기간이 90일을 넘으면 INVALID_INPUT이다.
public record AdminStatsRequest(
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

	static final int DEFAULT_DAYS = 7;
	static final int MAX_DAYS = 90;

	public Period resolve(LocalDate today) {
		LocalDate end = to == null ? today : to;
		LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1) : from;
		if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		return new Period(start, end);
	}

	public record Period(LocalDate from, LocalDate to) {
	}

}
