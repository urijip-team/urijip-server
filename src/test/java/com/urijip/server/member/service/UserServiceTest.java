package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void getUserReturnsSavedUser() {
		User saved = userRepository.saveAndFlush(User.builder()
				.email("mom@example.com")
				.password("encoded-password")
				.name("김엄마")
				.build());

		User found = userService.getUser(saved.getId());

		assertThat(found.getId()).isEqualTo(saved.getId());
		assertThat(found.getEmail()).isEqualTo("mom@example.com");
	}

	@Test
	void getUsersSkipsUnknownIds() {
		User mom = userRepository.save(User.builder()
				.email("mom@example.com")
				.password("encoded-password")
				.name("김엄마")
				.build());

		assertThat(userService.getUsers(List.of(mom.getId(), 999_999L)))
				.extracting(User::getId)
				.containsExactly(mom.getId());
	}

	@Test
	void getUserRejectsUnknownId() {
		assertThatThrownBy(() -> userService.getUser(999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

}
