package com.urijip.server.location.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.location.entity.UserLocation;

// 공유를 껐거나 아직 위치를 보낸 적이 없는 멤버는 latitude, longitude, updatedAt이 null이다.
public record FamilyLocationResponse(Long userId, String name, String nickname, String profileImageUrl,
		boolean sharingEnabled, Double latitude, Double longitude, OffsetDateTime updatedAt) {

	public static FamilyLocationResponse of(FamilyMemberResponse member, UserLocation location) {
		if (location == null) {
			return new FamilyLocationResponse(member.userId(), member.name(), member.nickname(),
					member.profileImageUrl(), true, null, null, null);
		}
		return new FamilyLocationResponse(member.userId(), member.name(), member.nickname(),
				member.profileImageUrl(), location.isSharingEnabled(), toDouble(location.getLatitude()),
				toDouble(location.getLongitude()), SeoulTime.toOffset(location.getUpdatedAt()));
	}

	private static Double toDouble(BigDecimal value) {
		return value == null ? null : value.doubleValue();
	}

}
