package com.urijip.server.stats.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AdminStatsResponseTest {

	@Test
	void ofFillsDaysWithoutSignupsWithZero() {
		AdminStatsResponse response = AdminStatsResponse.of(10, 3, 2, 1, LocalDate.of(2026, 11, 1),
				LocalDate.of(2026, 11, 4), Map.of(LocalDate.of(2026, 11, 2), 3L, LocalDate.of(2026, 11, 4), 1L));

		assertThat(response.signups()).containsExactly(new DailyCount(LocalDate.of(2026, 11, 1), 0),
				new DailyCount(LocalDate.of(2026, 11, 2), 3), new DailyCount(LocalDate.of(2026, 11, 3), 0),
				new DailyCount(LocalDate.of(2026, 11, 4), 1));
		assertThat(response.signupCount()).isEqualTo(4);
		assertThat(response.totalUsers()).isEqualTo(10);
		assertThat(response.totalFamilies()).isEqualTo(3);
		assertThat(response.pendingReports()).isEqualTo(2);
		assertThat(response.unansweredInquiries()).isEqualTo(1);
		assertThat(response.from()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(response.to()).isEqualTo(LocalDate.of(2026, 11, 4));
	}

	@Test
	void singleDayRangeHasOneEntry() {
		AdminStatsResponse response = AdminStatsResponse.of(0, 0, 0, 0, LocalDate.of(2026, 11, 8),
				LocalDate.of(2026, 11, 8), Map.of());

		assertThat(response.signups()).containsExactly(new DailyCount(LocalDate.of(2026, 11, 8), 0));
		assertThat(response.signupCount()).isZero();
	}

}
