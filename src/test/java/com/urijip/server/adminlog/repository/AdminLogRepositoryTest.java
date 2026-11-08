package com.urijip.server.adminlog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.adminlog.entity.AdminAction;
import com.urijip.server.adminlog.entity.AdminLog;
import com.urijip.server.adminlog.entity.AdminLogTargetType;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class AdminLogRepositoryTest {

	@Autowired
	private AdminLogRepository adminLogRepository;

	@Autowired
	private UserRepository userRepository;

	private User admin;

	@BeforeEach
	void setUp() {
		admin = saveUser("admin@example.com", "관리자");
	}

	@Test
	void savesLog() {
		AdminLog saved = adminLogRepository.saveAndFlush(new AdminLog(admin, AdminAction.CHANGE_USER_STATUS,
				AdminLogTargetType.USER, 7L, "ACTIVE -> SUSPENDED"));

		AdminLog found = adminLogRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getAdmin().getId()).isEqualTo(admin.getId());
		assertThat(found.getAction()).isEqualTo(AdminAction.CHANGE_USER_STATUS);
		assertThat(found.getTargetType()).isEqualTo(AdminLogTargetType.USER);
		assertThat(found.getTargetId()).isEqualTo(7L);
		assertThat(found.getDetail()).isEqualTo("ACTIVE -> SUSPENDED");
		assertThat(found.getCreatedAt()).isNotNull();
	}

	@Test
	void savesLongDetailCutToColumnLength() {
		AdminLog saved = adminLogRepository.saveAndFlush(new AdminLog(admin, AdminAction.CREATE_NOTICE,
				AdminLogTargetType.NOTICE, 1L, "가".repeat(300)));

		assertThat(adminLogRepository.findById(saved.getId()).orElseThrow().getDetail()).hasSize(255);
	}

	@Test
	void detailIsOptional() {
		AdminLog saved = adminLogRepository.saveAndFlush(new AdminLog(admin, AdminAction.ANSWER_INQUIRY,
				AdminLogTargetType.INQUIRY, 3L, null));

		assertThat(adminLogRepository.findById(saved.getId()).orElseThrow().getDetail()).isNull();
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
