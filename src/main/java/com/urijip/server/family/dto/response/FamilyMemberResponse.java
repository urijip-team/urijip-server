package com.urijip.server.family.dto.response;

import com.urijip.server.family.entity.FamilyRole;

public record FamilyMemberResponse(Long userId, String name, String profileImageUrl, String nickname,
		FamilyRole role) {

}
