package com.urijip.server.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void savedUserGetsDefaultsAndCreatedAt() {
		User saved = userRepository.saveAndFlush(user("mom@example.com"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getRole()).isEqualTo(Role.USER);
		assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(saved.getAdminLevel()).isNull();
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void existsByEmailFindsOnlySavedEmail() {
		userRepository.saveAndFlush(user("mom@example.com"));

		assertThat(userRepository.existsByEmail("mom@example.com")).isTrue();
		assertThat(userRepository.existsByEmail("dad@example.com")).isFalse();
	}

	@Test
	void duplicatedEmailIsRejectedByUniqueConstraint() {
		userRepository.saveAndFlush(user("mom@example.com"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(user("mom@example.com")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findByEmailReturnsSavedUser() {
		User saved = userRepository.saveAndFlush(user("mom@example.com"));

		assertThat(userRepository.findByEmail("mom@example.com"))
				.get()
				.extracting(User::getId)
				.isEqualTo(saved.getId());
	}

	private static User user(String email) {
		return User.builder()
				.email(email)
				.password("encoded-password")
				.name("김엄마")
				.build();
	}

}
