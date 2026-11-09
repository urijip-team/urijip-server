package com.urijip.server.adminlog.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.adminlog.entity.AdminAction;
import com.urijip.server.adminlog.entity.AdminLog;
import com.urijip.server.adminlog.entity.AdminLogTargetType;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;

public record AdminLogResponse(Long id, Admin admin, AdminAction action, AdminLogTargetType targetType, Long targetId,
		String detail, OffsetDateTime createdAt) {

	public record Admin(Long id, String name) {
	}

	public static AdminLogResponse from(AdminLog log) {
		User admin = log.getAdmin();
		return new AdminLogResponse(log.getId(), new Admin(admin.getId(), admin.getName()), log.getAction(),
				log.getTargetType(), log.getTargetId(), log.getDetail(), SeoulTime.toOffset(log.getCreatedAt()));
	}

}
