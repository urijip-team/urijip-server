package com.urijip.server.family.dto.response;

import com.urijip.server.family.entity.Family;

public record FamilyResponse(Long id, String name, String inviteCode) {

	public static FamilyResponse from(Family family) {
		return new FamilyResponse(family.getId(), family.getName(), family.getInviteCode());
	}

}
