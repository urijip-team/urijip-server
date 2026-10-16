package com.urijip.server.global.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 엔티티의 {@link LocalDateTime}(서울 시간)과 API의 {@link OffsetDateTime}(+09:00)을 서로 바꾼다.
 * 테스트·CI는 {@code main}을 거치지 않아 JVM 기본 시간대가 다를 수 있으므로 시간대를 직접 지정한다.
 */
public final class SeoulTime {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	private SeoulTime() {
	}

	public static LocalDateTime toLocal(OffsetDateTime dateTime) {
		return dateTime == null ? null : dateTime.atZoneSameInstant(ZONE).toLocalDateTime();
	}

	public static OffsetDateTime toOffset(LocalDateTime dateTime) {
		return dateTime == null ? null : dateTime.atZone(ZONE).toOffsetDateTime();
	}

}
