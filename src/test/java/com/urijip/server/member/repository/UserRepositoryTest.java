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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

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

	@Test
	void findByEmailReturnsEmptyForUnknownEmail() {
		userRepository.saveAndFlush(user("mom@example.com"));

		assertThat(userRepository.findByEmail("dad@example.com")).isEmpty();
	}

	@Test
	void searchFindsKeywordInEmailOrName() {
		User byEmail = userRepository.saveAndFlush(user("kim.mom@example.com", "엄마"));
		User byName = userRepository.saveAndFlush(user("dad@example.com", "김아빠"));
		userRepository.saveAndFlush(user("son@example.com", "아들"));

		assertThat(search(null, "kim").getContent()).extracting(User::getId).containsExactly(byEmail.getId());
		assertThat(search(null, "김아").getContent()).extracting(User::getId).containsExactly(byName.getId());
		assertThat(search(null, "MOM").getContent()).extracting(User::getId).containsExactly(byEmail.getId());
	}

	@Test
	void searchWithoutConditionsReturnsAllUsersNewestFirst() {
		User mom = userRepository.saveAndFlush(user("mom@example.com", "엄마"));
		User dad = userRepository.saveAndFlush(user("dad@example.com", "아빠"));

		assertThat(search(null, null).getContent()).extracting(User::getId).containsExactly(dad.getId(), mom.getId());
	}

	@Test
	void searchFiltersByStatus() {
		userRepository.saveAndFlush(user("mom@example.com", "엄마"));
		User suspended = userRepository.saveAndFlush(user("dad@example.com", "아빠"));
		suspended.changeStatus(UserStatus.SUSPENDED);
		userRepository.flush();

		assertThat(search(UserStatus.SUSPENDED, null).getContent()).extracting(User::getId)
				.containsExactly(suspended.getId());
	}

	@Test
	void searchExcludesAdmins() {
		User admin = userRepository.saveAndFlush(user("admin@example.com", "관리자"));
		ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
		userRepository.flush();

		assertThat(search(null, null).getContent()).isEmpty();
	}

	@Test
	void searchTreatsEscapedWildcardsLiterally() {
		User underscore = userRepository.saveAndFlush(user("a_b@example.com", "엄마"));
		userRepository.saveAndFlush(user("axb@example.com", "아빠"));
		User percent = userRepository.saveAndFlush(user("p@example.com", "100%"));
		User bang = userRepository.saveAndFlush(user("hi!@example.com", "누나"));

		assertThat(search(null, "a!_b").getContent()).extracting(User::getId).containsExactly(underscore.getId());
		assertThat(search(null, "0!%").getContent()).extracting(User::getId).containsExactly(percent.getId());
		assertThat(search(null, "hi!!").getContent()).extracting(User::getId).containsExactly(bang.getId());
	}

	@Test
	void searchPagesResults() {
		for (int i = 0; i < 3; i++) {
			userRepository.saveAndFlush(user("user" + i + "@example.com", "회원" + i));
		}

		Page<User> page = userRepository.search(Role.USER, null, null,
				PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "id")));

		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	private Page<User> search(UserStatus status, String keyword) {
		return userRepository.search(Role.USER, status, keyword, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC,
				"id")));
	}

	private static User user(String email, String name) {
		return User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build();
	}

	private static User user(String email) {
		return User.builder()
				.email(email)
				.password("encoded-password")
				.name("김엄마")
				.build();
	}

}
