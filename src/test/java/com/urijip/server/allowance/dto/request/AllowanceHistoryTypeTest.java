package com.urijip.server.allowance.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AllowanceHistoryTypeTest {

	@Test
	void fromReadsLowerCaseAsInSpec() {
		assertThat(AllowanceHistoryType.from("sent")).isEqualTo(AllowanceHistoryType.SENT);
		assertThat(AllowanceHistoryType.from("received")).isEqualTo(AllowanceHistoryType.RECEIVED);
	}

	@Test
	void fromIgnoresCaseAndSpaces() {
		assertThat(AllowanceHistoryType.from(" RECEIVED ")).isEqualTo(AllowanceHistoryType.RECEIVED);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "all", "sentt"})
	void fromRejectsUnknownValue(String value) {
		assertThatThrownBy(() -> AllowanceHistoryType.from(value))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
	}

}
