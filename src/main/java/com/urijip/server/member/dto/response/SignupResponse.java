package com.urijip.server.member.dto.response;

import com.urijip.server.member.entity.User;

public record SignupResponse(Long id, String email, String name) {

	public static SignupResponse from(User user) {
		return new SignupResponse(user.getId(), user.getEmail(), user.getName());
	}

}
