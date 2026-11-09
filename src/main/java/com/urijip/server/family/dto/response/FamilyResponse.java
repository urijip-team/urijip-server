package com.urijip.server.family.dto.response;

import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyStatus;

// status가 SUSPENDED면 관리자가 정지한 가족이다. 앱은 SOS·위치 공유만 열어 둔다.
public record FamilyResponse(Long id, String name, String inviteCode, FamilyStatus status) {

	public static FamilyResponse from(Family family) {
		return new FamilyResponse(family.getId(), family.getName(), family.getInviteCode(), family.getStatus());
	}

}
