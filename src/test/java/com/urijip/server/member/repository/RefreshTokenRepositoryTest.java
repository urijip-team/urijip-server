package com.urijip.server.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.RefreshToken;
import com.urijip.server.member.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class RefreshTokenRepositoryTest {

	private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 10, 24, 12, 0);

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserRepository userRepository;

	private User user;

	@BeforeEach
	void setUp() {
		user = userRepository.saveAndFlush(User.builder()
				.email("mom@example.com")
				.password("encoded-password")
				.name("김엄마")
				.build());
	}

	@Test
	void savedTokenKeepsUserTokenAndExpiry() {
		RefreshToken saved = refreshTokenRepository.saveAndFlush(new RefreshToken(user, "token-1", EXPIRES_AT));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getUser().getId()).isEqualTo(user.getId());
		assertThat(saved.getToken()).isEqualTo("token-1");
		assertThat(saved.getExpiresAt()).isEqualTo(EXPIRES_AT);
	}

	@Test
	void tokenLongerThanDefaultColumnLengthIsSaved() {
		String longToken = "a".repeat(400);

		RefreshToken saved = refreshTokenRepository.saveAndFlush(new RefreshToken(user, longToken, EXPIRES_AT));

		assertThat(saved.getToken()).hasSize(400);
	}

	@Test
	void oneUserCanHaveSeveralTokens() {
		refreshTokenRepository.saveAndFlush(new RefreshToken(user, "token-1", EXPIRES_AT));
		refreshTokenRepository.saveAndFlush(new RefreshToken(user, "token-2", EXPIRES_AT));

		assertThat(refreshTokenRepository.count()).isEqualTo(2);
	}

	@Test
	void duplicatedTokenIsRejectedByUniqueConstraint() {
		refreshTokenRepository.saveAndFlush(new RefreshToken(user, "token-1", EXPIRES_AT));

		assertThatThrownBy(() -> refreshTokenRepository
				.saveAndFlush(new RefreshToken(user, "token-1", EXPIRES_AT)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
