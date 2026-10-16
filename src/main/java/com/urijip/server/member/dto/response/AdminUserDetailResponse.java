package com.urijip.server.member.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;

// 관리자용 회원 상세. 위치·채팅·계좌 정보는 넣지 않는다(공통 정책).
public record AdminUserDetailResponse(Long id, String email, String name, String profileImageUrl,
		LocalDate birthday, String statusMessage, UserStatus status, OffsetDateTime createdAt, Family family) {

	public static AdminUserDetailResponse of(User user, FamilyResponse family) {
		return new AdminUserDetailResponse(user.getId(), user.getEmail(), user.getName(), user.getProfileImageUrl(),
				user.getBirthday(), user.getStatusMessage(), user.getStatus(), SeoulTime.toOffset(user.getCreatedAt()),
				family == null ? null : new Family(family.id(), family.name()));
	}

	public record Family(Long id, String name) {
	}

}
