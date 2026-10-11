package com.urijip.server.member.dto.response;

import com.urijip.server.member.entity.AdminLevel;

public record AdminLoginResponse(String accessToken, String refreshToken, AdminInfo admin) {

	public record AdminInfo(Long id, String name, AdminLevel adminLevel) {
	}

}
