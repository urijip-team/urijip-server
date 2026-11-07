package com.urijip.server.notice.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.member.entity.User;
import com.urijip.server.notice.entity.Notice;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NoticeResponseTest {

	@Test
	void fromCopiesNoticeWithoutAdminFields() {
		User admin = User.builder().email("admin@example.com").password("pw").name("관리자").build();
		Notice notice = new Notice(admin, "서버 점검 안내", "내일 새벽 2시에 점검합니다.");
		ReflectionTestUtils.setField(notice, "id", 3L);

		assertThat(NoticeResponse.from(notice)).isEqualTo(new NoticeResponse(3L, "서버 점검 안내", "내일 새벽 2시에 점검합니다."));
	}

}
