package com.urijip.server.sos.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;
import com.urijip.server.sos.entity.SosAlert;
import com.urijip.server.sos.entity.SosStatus;

// SOS 기록과 STOMP /sub/family/{familyId}/sos 가 같은 형식을 쓴다. resolvedBy, resolvedAt은 해제 전에는 null이다.
// 이름은 가족 안 호칭을 먼저 쓰고, 가족을 나간 회원은 회원 이름을 쓴다.
public record SosResponse(Long sosId, Long userId, String userName, Double latitude, Double longitude,
		SosStatus status, OffsetDateTime createdAt, ResolvedBy resolvedBy, OffsetDateTime resolvedAt) {

	public record ResolvedBy(Long userId, String name) {
	}

	public static SosResponse of(SosAlert alert, Map<Long, String> names) {
		User user = alert.getUser();
		User resolver = alert.getResolvedBy();
		return new SosResponse(alert.getId(), user.getId(), names.getOrDefault(user.getId(), user.getName()),
				alert.getLatitude().doubleValue(), alert.getLongitude().doubleValue(), alert.getStatus(),
				SeoulTime.toOffset(alert.getCreatedAt()),
				resolver == null ? null
						: new ResolvedBy(resolver.getId(), names.getOrDefault(resolver.getId(), resolver.getName())),
				SeoulTime.toOffset(alert.getResolvedAt()));
	}

}
