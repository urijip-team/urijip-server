package com.urijip.server.member.dto.response;

import com.urijip.server.member.entity.AdminLevel;
import com.urijip.server.member.entity.User;

public record AdminLoginResponse(String accessToken, String refreshToken, AdminInfo admin) {

	public static AdminLoginResponse of(String accessToken, String refreshToken, User admin) {
		return new AdminLoginResponse(accessToken, refreshToken,
				new AdminInfo(admin.getId(), admin.getName(), admin.getAdminLevel()));
	}

	public record AdminInfo(Long id, String name, AdminLevel adminLevel) {
	}

}
