package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserStatusServiceTest {

	@Autowired
	private UserStatusService userStatusService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

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
		userStatusService.evict(mom.getId());

		assertError(mom.getId(), ErrorCode.USER_SUSPENDED);
	}

	@Test
	void withdrawnOrUnknownUserIsRejectedAsInvalidToken() {
		mom.withdraw();
		userRepository.flush();
		userStatusService.evict(mom.getId());

		assertError(mom.getId(), ErrorCode.INVALID_TOKEN);
		assertError(-1L, ErrorCode.INVALID_TOKEN);
	}

	@Test
	void statusIsCachedUntilEvicted() {
		userStatusService.validateActive(mom.getId());
		mom.changeStatus(UserStatus.SUSPENDED);
		userRepository.flush();

		assertThatCode(() -> userStatusService.validateActive(mom.getId())).doesNotThrowAnyException();

		userStatusService.evict(mom.getId());
		assertError(mom.getId(), ErrorCode.USER_SUSPENDED);
	}

	// 정지를 커밋하기 전에 다른 요청이 옛 상태(ACTIVE)를 기억해 두어도 커밋 뒤에는 막힌다.
	// 트랜잭션 없이 돌아서 setUp의 회원도 커밋되므로 끝나면 지운다.
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void statusCachedBeforeCommitIsEvictedAfterCommit() {
		try {
			transactionTemplate.executeWithoutResult(status -> {
				userRepository.findById(mom.getId()).orElseThrow().changeStatus(UserStatus.SUSPENDED);
				userRepository.flush();
				userStatusService.evict(mom.getId());
				CompletableFuture.runAsync(() -> userStatusService.validateActive(mom.getId())).join();
			});

			assertError(mom.getId(), ErrorCode.USER_SUSPENDED);
		} finally {
			userRepository.deleteById(mom.getId());
			userStatusService.evict(mom.getId());
		}
	}

	private void assertError(Long userId, ErrorCode errorCode) {
		assertThatThrownBy(() -> userStatusService.validateActive(userId))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(errorCode));
	}

}
