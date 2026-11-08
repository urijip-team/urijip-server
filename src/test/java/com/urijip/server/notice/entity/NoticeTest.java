package com.urijip.server.notice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.member.entity.User;
import org.junit.jupiter.api.Test;

class NoticeTest {

	private final User admin = User.builder().email("admin@example.com").password("pw").name("관리자").build();

	@Test
	void updateChangesBothFields() {
		Notice notice = new Notice(admin, "제목", "내용");

		notice.update("새 제목", "새 내용");

		assertThat(notice.getTitle()).isEqualTo("새 제목");
		assertThat(notice.getContent()).isEqualTo("새 내용");
	}

	@Test
	void updateKeepsFieldsThatAreNull() {
		Notice notice = new Notice(admin, "제목", "내용");

		notice.update(null, "새 내용");
		assertThat(notice.getTitle()).isEqualTo("제목");
		assertThat(notice.getContent()).isEqualTo("새 내용");

		notice.update("새 제목", null);
		assertThat(notice.getTitle()).isEqualTo("새 제목");
		assertThat(notice.getContent()).isEqualTo("새 내용");
	}

	@Test
	void newNoticeIsNotPushedUntilMarked() {
		Notice notice = new Notice(admin, "제목", "내용");
		assertThat(notice.isPushed()).isFalse();

		notice.markPushed();

		assertThat(notice.isPushed()).isTrue();
	}

}
