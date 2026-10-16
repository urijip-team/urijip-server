package com.urijip.server.member.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;

public record AdminUserResponse(Long id, String email, String name, UserStatus status, OffsetDateTime createdAt) {

	public static AdminUserResponse from(User user) {
		return new AdminUserResponse(user.getId(), user.getEmail(), user.getName(), user.getStatus(),
				SeoulTime.toOffset(user.getCreatedAt()));
	}

}
