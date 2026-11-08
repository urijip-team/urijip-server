package com.urijip.server.stats.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 카드 숫자(totalUsers 등)는 조회 시점 기준이고, signups는 from~to(둘 다 포함)의 날짜별 가입 수다.
public record AdminStatsResponse(long totalUsers, long totalFamilies, long pendingReports, long unansweredInquiries,
		LocalDate from, LocalDate to, long signupCount, List<DailyCount> signups) {

	// 가입이 없는 날도 0으로 넣어 차트가 날짜를 빠뜨리지 않게 한다.
	public static AdminStatsResponse of(long totalUsers, long totalFamilies, long pendingReports,
			long unansweredInquiries, LocalDate from, LocalDate to, Map<LocalDate, Long> signupsByDay) {
		List<DailyCount> signups = from.datesUntil(to.plusDays(1))
				.map(date -> new DailyCount(date, signupsByDay.getOrDefault(date, 0L)))
				.toList();
		long signupCount = signups.stream().mapToLong(DailyCount::count).sum();
		return new AdminStatsResponse(totalUsers, totalFamilies, pendingReports, unansweredInquiries, from, to,
				signupCount, signups);
	}

}
