package com.urijip.server.notice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notice.entity.Notice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class NoticeRepositoryTest {

	@Autowired
	private NoticeRepository noticeRepository;

	@Autowired
	private UserRepository userRepository;

	private User admin;

	@BeforeEach
	void setUp() {
		admin = userRepository.saveAndFlush(User.builder()
				.email("admin@example.com")
				.password("encoded-password")
				.name("관리자")
				.build());
	}

	@Test
	void savesNoticeNotPushed() {
		Notice notice = noticeRepository.saveAndFlush(new Notice(admin, "서버 점검 안내", "내일 새벽 2시에 점검합니다."));

		Notice found = noticeRepository.findById(notice.getId()).orElseThrow();
		assertThat(found.getTitle()).isEqualTo("서버 점검 안내");
		assertThat(found.getContent()).isEqualTo("내일 새벽 2시에 점검합니다.");
		assertThat(found.isPushed()).isFalse();
		assertThat(found.getCreatedBy().getId()).isEqualTo(admin.getId());
	}

}
