package com.urijip.server.location.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.location.entity.UserLocation;

// STOMP /sub/family/{familyId}/location 으로 보내는 메시지. 공유를 끄면 sharingEnabled false와 null 위치가 간다.
public record LocationChangedMessage(Long userId, boolean sharingEnabled, Double latitude, Double longitude,
		OffsetDateTime updatedAt) {

	public static LocationChangedMessage from(UserLocation location) {
		return new LocationChangedMessage(location.getUserId(), location.isSharingEnabled(),
				toDouble(location.getLatitude()), toDouble(location.getLongitude()),
				SeoulTime.toOffset(location.getUpdatedAt()));
	}

	private static Double toDouble(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}

}
