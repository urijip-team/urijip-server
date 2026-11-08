package com.urijip.server.allowance.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.urijip.server.allowance.entity.AllowanceStatus;
import org.junit.jupiter.api.Test;

class AllowanceMonthlyResponseTest {

	@Test
	void totalCountsOnlySentRequests() {
		List<AllowanceResponse> requests = List.of(response(1L, 30000, AllowanceStatus.SENT),
				response(2L, 20000, AllowanceStatus.REQUESTED), response(3L, 50000, AllowanceStatus.REJECTED),
				response(4L, 5000, AllowanceStatus.SENT));

		AllowanceMonthlyResponse monthly = AllowanceMonthlyResponse.from(requests);

		assertThat(monthly.requests()).isEqualTo(requests);
		assertThat(monthly.totalAmount()).isEqualTo(35000);
	}

	@Test
	void totalDoesNotOverflowInt() {
		List<AllowanceResponse> requests = List.of(response(1L, 10_000_000, AllowanceStatus.SENT),
				response(2L, Integer.MAX_VALUE, AllowanceStatus.SENT));

		assertThat(AllowanceMonthlyResponse.from(requests).totalAmount()).isEqualTo(10_000_000L + Integer.MAX_VALUE);
	}

	@Test
	void emptyMonthHasZeroTotal() {
		assertThat(AllowanceMonthlyResponse.from(List.of())).isEqualTo(new AllowanceMonthlyResponse(List.of(), 0));
	}

	private AllowanceResponse response(Long id, int amount, AllowanceStatus status) {
		return new AllowanceResponse(id, 3L, new AllowanceResponse.Person(2L, "김첫째"),
				new AllowanceResponse.Person(1L, "김엄마"), amount, null, status, null, null);
	}

}
