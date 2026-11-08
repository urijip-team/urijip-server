package com.urijip.server.stats.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class AdminStatsRequestTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

	@Test
	void defaultsToLastSevenDaysIncludingToday() {
		assertThat(new AdminStatsRequest(null, null).resolve(TODAY))
				.isEqualTo(new AdminStatsRequest.Period(LocalDate.of(2026, 11, 2), TODAY));
	}

	@Test
	void missingFromIsSevenDaysBeforeTo() {
		assertThat(new AdminStatsRequest(null, LocalDate.of(2026, 10, 31)).resolve(TODAY))
				.isEqualTo(new AdminStatsRequest.Period(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 31)));
	}

	@Test
	void missingToIsToday() {
		assertThat(new AdminStatsRequest(LocalDate.of(2026, 11, 1), null).resolve(TODAY))
				.isEqualTo(new AdminStatsRequest.Period(LocalDate.of(2026, 11, 1), TODAY));
	}

	@Test
	void sameDayIsAllowed() {
		assertThat(new AdminStatsRequest(TODAY, TODAY).resolve(TODAY)).isEqualTo(new AdminStatsRequest.Period(TODAY, TODAY));
	}

	@Test
	void ninetyDaysIsAllowed() {
		LocalDate from = TODAY.minusDays(89);

		assertThat(new AdminStatsRequest(from, TODAY).resolve(TODAY)).isEqualTo(new AdminStatsRequest.Period(from, TODAY));
	}

	@Test
	void moreThanNinetyDaysFails() {
		assertInvalid(new AdminStatsRequest(TODAY.minusDays(90), TODAY));
	}

	@Test
	void fromAfterToFails() {
		assertInvalid(new AdminStatsRequest(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 4)));
	}

	@Test
	void fromAfterTodayWithoutToFails() {
		assertInvalid(new AdminStatsRequest(TODAY.plusDays(1), null));
	}

	private void assertInvalid(AdminStatsRequest request) {
		assertThatThrownBy(() -> request.resolve(TODAY))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
	}

}
