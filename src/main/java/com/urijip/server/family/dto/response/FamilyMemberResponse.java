package com.urijip.server.family.dto.response;

import com.urijip.server.family.entity.FamilyMember;
import com.urijip.server.family.entity.FamilyRole;
import com.urijip.server.member.entity.User;

public record FamilyMemberResponse(Long userId, String name, String profileImageUrl, String nickname,
		FamilyRole role) {

	public static FamilyMemberResponse from(FamilyMember member) {
		User user = member.getUser();
		return new FamilyMemberResponse(user.getId(), user.getName(), user.getProfileImageUrl(),
				member.getNickname(), member.getRole());
	}

}
