package com.urijip.server.family.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyStatus;
import com.urijip.server.global.time.SeoulTime;

public record AdminFamilyResponse(Long id, String name, long memberCount, FamilyStatus status,
		OffsetDateTime createdAt) {

	public static AdminFamilyResponse of(Family family, long memberCount) {
		return new AdminFamilyResponse(family.getId(), family.getName(), memberCount, family.getStatus(),
				SeoulTime.toOffset(family.getCreatedAt()));
	}

}
