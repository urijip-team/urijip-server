package com.urijip.server.global.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class SeoulTimeTest {

	@Test
	void toLocalKeepsSeoulTime() {
		OffsetDateTime seoul = OffsetDateTime.of(2026, 10, 2, 14, 0, 0, 0, ZoneOffset.ofHours(9));

		assertThat(SeoulTime.toLocal(seoul)).isEqualTo(LocalDateTime.of(2026, 10, 2, 14, 0));
	}

	@Test
	void toLocalConvertsOtherOffsetToSeoulTime() {
		OffsetDateTime utc = OffsetDateTime.of(2026, 10, 2, 5, 0, 0, 0, ZoneOffset.UTC);

		assertThat(SeoulTime.toLocal(utc)).isEqualTo(LocalDateTime.of(2026, 10, 2, 14, 0));
	}

	@Test
	void toOffsetAddsSeoulOffset() {
		OffsetDateTime result = SeoulTime.toOffset(LocalDateTime.of(2026, 10, 2, 14, 0));

		assertThat(result).isEqualTo(OffsetDateTime.of(2026, 10, 2, 14, 0, 0, 0, ZoneOffset.ofHours(9)));
		assertThat(result.toString()).isEqualTo("2026-10-02T14:00+09:00");
	}

	@Test
	void nullStaysNull() {
		assertThat(SeoulTime.toLocal(null)).isNull();
		assertThat(SeoulTime.toOffset(null)).isNull();
	}

}
