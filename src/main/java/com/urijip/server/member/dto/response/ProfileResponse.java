package com.urijip.server.member.dto.response;

import java.time.LocalDate;

import com.urijip.server.member.entity.User;

public record ProfileResponse(Long id, String name, String profileImageUrl, LocalDate birthday,
		String statusMessage) {

	public static ProfileResponse from(User user) {
		return new ProfileResponse(user.getId(), user.getName(), user.getProfileImageUrl(), user.getBirthday(),
				user.getStatusMessage());
	}

}
