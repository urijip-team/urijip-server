package com.urijip.server.member.dto.response;

public record LoginResponse(String accessToken, String refreshToken, UserInfo user) {

	public record UserInfo(Long id, String name, boolean hasFamily) {
	}

}
