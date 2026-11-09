package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserStatusServiceTest {

	@Autowired
	private UserStatusService userStatusService;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	@BeforeEach
	void setUp() {
		mom = userRepository.saveAndFlush(User.builder().email("mom@example.com").password("pw").name("김엄마").build());
	}

	@Test
	void activeUserPasses() {
		assertThatCode(() -> userStatusService.validateActive(mom.getId())).doesNotThrowAnyException();
	}

	@Test
	void suspendedUserIsRejectedAsSuspended() {
		mom.changeStatus(UserStatus.SUSPENDED);
		userRepository.flush();

		assertError(mom.getId(), ErrorCode.USER_SUSPENDED);
	}

	@Test
	void withdrawnOrUnknownUserIsRejectedAsInvalidToken() {
		mom.withdraw();
		userRepository.flush();

		assertError(mom.getId(), ErrorCode.INVALID_TOKEN);
		assertError(-1L, ErrorCode.INVALID_TOKEN);
	}

	private void assertError(Long userId, ErrorCode errorCode) {
		assertThatThrownBy(() -> userStatusService.validateActive(userId))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(errorCode));
	}

}
