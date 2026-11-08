package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
	void countUsersSkipsWithdrawn() {
		long before = userService.countUsers();
		userRepository.save(newUser("count-a@example.com"));
		User suspended = userRepository.save(newUser("count-b@example.com"));
		suspended.changeStatus(UserStatus.SUSPENDED);
		User withdrawn = userRepository.save(newUser("count-c@example.com"));
		withdrawn.withdraw();
		userRepository.flush();

		assertThat(userService.countUsers()).isEqualTo(before + 2);
	}

	@Test
	void countSignupsByDayIncludesBothEnds() {
		signupAt("first@example.com", LocalDateTime.of(2020, 1, 1, 0, 0));
		signupAt("last@example.com", LocalDateTime.of(2020, 1, 3, 23, 59, 59));
		signupAt("out@example.com", LocalDateTime.of(2020, 1, 4, 0, 0));

		Map<LocalDate, Long> counts = userService.countSignupsByDay(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 3));

		assertThat(counts).containsExactly(Map.entry(LocalDate.of(2020, 1, 1), 1L),
				Map.entry(LocalDate.of(2020, 1, 3), 1L));
	}

	@Test
	void getUserRejectsUnknownId() {
		assertThatThrownBy(() -> userService.getUser(999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	private void signupAt(String email, LocalDateTime createdAt) {
		User user = userRepository.saveAndFlush(newUser(email));
		jdbcTemplate.update("update users set created_at = ? where id = ?", createdAt, user.getId());
	}

	private static User newUser(String email) {
		return User.builder().email(email).password("encoded-password").name("회원").build();
	}

}
