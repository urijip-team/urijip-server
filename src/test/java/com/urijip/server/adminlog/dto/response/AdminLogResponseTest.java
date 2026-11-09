package com.urijip.server.adminlog.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.urijip.server.adminlog.entity.AdminAction;
import com.urijip.server.adminlog.entity.AdminLog;
import com.urijip.server.adminlog.entity.AdminLogTargetType;
import com.urijip.server.member.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AdminLogResponseTest {

	@Test
	void fromCopiesLogWithSeoulOffset() {
		User admin = User.builder().email("admin@example.com").password("pw").name("관리자").build();
		ReflectionTestUtils.setField(admin, "id", 100L);
		AdminLog log = new AdminLog(admin, AdminAction.HANDLE_REPORT, AdminLogTargetType.REPORT, 9L, "DELETE_PHOTO");
		ReflectionTestUtils.setField(log, "id", 1L);
		ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 11, 8, 10, 0));

		assertThat(AdminLogResponse.from(log)).isEqualTo(new AdminLogResponse(1L,
				new AdminLogResponse.Admin(100L, "관리자"), AdminAction.HANDLE_REPORT, AdminLogTargetType.REPORT, 9L,
				"DELETE_PHOTO", OffsetDateTime.parse("2026-11-08T10:00:00+09:00")));
	}

}
