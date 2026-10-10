package com.urijip.server.member.dto.response;

import com.urijip.server.member.entity.User;

public record LoginResponse(String accessToken, String refreshToken, UserInfo user) {

	public static LoginResponse of(String accessToken, String refreshToken, User user, boolean hasFamily) {
		return new LoginResponse(accessToken, refreshToken,
				new UserInfo(user.getId(), user.getName(), hasFamily));
	}

	public record UserInfo(Long id, String name, boolean hasFamily) {
	}

}
