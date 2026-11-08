package com.urijip.server.adminlog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.adminlog.entity.AdminAction;
import com.urijip.server.adminlog.entity.AdminLogTargetType;
import com.urijip.server.adminlog.repository.AdminLogRepository;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminLogServiceTest {

	@Autowired
	private AdminLogService adminLogService;

	@Autowired
	private AdminLogRepository adminLogRepository;

	@Autowired
	private UserRepository userRepository;

	private User admin;

	@BeforeEach
	void setUp() {
		admin = saveAdmin("admin@example.com", "관리자");
	}

	@Test
	void recordSavesLog() {
		adminLogService.record(admin.getId(), AdminAction.CHANGE_FAMILY_STATUS, AdminLogTargetType.FAMILY, 3L,
				"ACTIVE -> SUSPENDED");

		assertThat(adminLogRepository.findAll()).singleElement().satisfies(log -> {
			assertThat(log.getAdmin().getId()).isEqualTo(admin.getId());
			assertThat(log.getAction()).isEqualTo(AdminAction.CHANGE_FAMILY_STATUS);
			assertThat(log.getTargetType()).isEqualTo(AdminLogTargetType.FAMILY);
			assertThat(log.getTargetId()).isEqualTo(3L);
			assertThat(log.getDetail()).isEqualTo("ACTIVE -> SUSPENDED");
		});
	}

	@Test
	void recordByUnknownAdminFails() {
		assertThatThrownBy(() -> adminLogService.record(-1L, AdminAction.DELETE_NOTICE, AdminLogTargetType.NOTICE,
				1L, null))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
		assertThat(adminLogRepository.count()).isZero();
	}

	private User saveAdmin(String email, String name) {
		User user = userRepository.save(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
		ReflectionTestUtils.setField(user, "role", Role.ADMIN);
		return user;
	}

}
