package com.urijip.server.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.DeviceToken;
import com.urijip.server.member.entity.Platform;
import com.urijip.server.member.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class DeviceTokenRepositoryTest {

	@Autowired
	private DeviceTokenRepository deviceTokenRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
	}

	@Test
	void savedTokenKeepsUserTokenAndPlatform() {
		DeviceToken saved = deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.IOS));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getUser().getId()).isEqualTo(mom.getId());
		assertThat(saved.getFcmToken()).isEqualTo("fcm-1");
		assertThat(saved.getPlatform()).isEqualTo(Platform.IOS);
	}

	@Test
	void savedTokenGetsUpdatedAt() {
		DeviceToken saved = deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.IOS));

		assertThat(saved.getUpdatedAt()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
	}

	@Test
	void oneUserCanHaveSeveralTokens() {
		deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.ANDROID));
		deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-2", Platform.IOS));

		assertThat(deviceTokenRepository.count()).isEqualTo(2);
	}

	@Test
	void duplicatedFcmTokenIsRejectedByUniqueConstraint() {
		deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.ANDROID));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> deviceTokenRepository
				.saveAndFlush(new DeviceToken(dad, "fcm-1", Platform.ANDROID)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findByFcmTokenReturnsSavedToken() {
		DeviceToken saved = deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.ANDROID));

		assertThat(deviceTokenRepository.findByFcmToken("fcm-1"))
				.get()
				.extracting(DeviceToken::getId)
				.isEqualTo(saved.getId());
	}

	@Test
	void findByFcmTokenReturnsEmptyForUnknownToken() {
		deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.ANDROID));

		assertThat(deviceTokenRepository.findByFcmToken("fcm-2")).isEmpty();
	}

	@Test
	void deleteByFcmTokenAndUserIdDeletesOwnToken() {
		deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.ANDROID));

		long deleted = deviceTokenRepository.deleteByFcmTokenAndUserId("fcm-1", mom.getId());

		assertThat(deleted).isEqualTo(1);
		assertThat(deviceTokenRepository.count()).isZero();
	}

	@Test
	void deleteByFcmTokenAndUserIdKeepsTokenOfAnotherUser() {
		deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.ANDROID));
		User dad = saveUser("dad@example.com", "김아빠");

		long deleted = deviceTokenRepository.deleteByFcmTokenAndUserId("fcm-1", dad.getId());

		assertThat(deleted).isZero();
		assertThat(deviceTokenRepository.count()).isEqualTo(1);
	}

	@Test
	void deleteByUserIdDeletesAllTokensOfUser() {
		deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.ANDROID));
		deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-2", Platform.IOS));

		long deleted = deviceTokenRepository.deleteByUserId(mom.getId());

		assertThat(deleted).isEqualTo(2);
		assertThat(deviceTokenRepository.count()).isZero();
	}

	@Test
	void deleteByUserIdKeepsTokensOfAnotherUser() {
		deviceTokenRepository.saveAndFlush(new DeviceToken(mom, "fcm-1", Platform.ANDROID));
		User dad = saveUser("dad@example.com", "김아빠");

		long deleted = deviceTokenRepository.deleteByUserId(dad.getId());

		assertThat(deleted).isZero();
		assertThat(deviceTokenRepository.count()).isEqualTo(1);
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
