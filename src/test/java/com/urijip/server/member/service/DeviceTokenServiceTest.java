package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.dto.request.DeviceTokenRequest;
import com.urijip.server.member.entity.DeviceToken;
import com.urijip.server.member.entity.Platform;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.DeviceTokenRepository;
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
class DeviceTokenServiceTest {

	@Autowired
	private DeviceTokenService deviceTokenService;

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
	void registerSavesTokenOfUser() {
		deviceTokenService.register(mom.getId(), new DeviceTokenRequest("fcm-1", Platform.ANDROID));

		assertThat(deviceTokenRepository.findAll()).singleElement().satisfies(saved -> {
			assertThat(saved.getUser().getId()).isEqualTo(mom.getId());
			assertThat(saved.getFcmToken()).isEqualTo("fcm-1");
			assertThat(saved.getPlatform()).isEqualTo(Platform.ANDROID);
		});
	}

	@Test
	void registerKeepsTokensOfOtherDevices() {
		deviceTokenService.register(mom.getId(), new DeviceTokenRequest("fcm-1", Platform.ANDROID));

		deviceTokenService.register(mom.getId(), new DeviceTokenRequest("fcm-2", Platform.IOS));

		assertThat(deviceTokenRepository.findAll())
				.extracting(DeviceToken::getFcmToken)
				.containsExactlyInAnyOrder("fcm-1", "fcm-2");
	}

	@Test
	void registerSameTokenAgainKeepsSingleRow() {
		deviceTokenService.register(mom.getId(), new DeviceTokenRequest("fcm-1", Platform.ANDROID));

		deviceTokenService.register(mom.getId(), new DeviceTokenRequest("fcm-1", Platform.ANDROID));

		assertThat(deviceTokenRepository.count()).isEqualTo(1);
	}

	@Test
	void registerSameTokenAgainRefreshesUpdatedAt() {
		deviceTokenService.register(mom.getId(), new DeviceTokenRequest("fcm-1", Platform.ANDROID));
		LocalDateTime old = LocalDateTime.now().minusDays(30);
		DeviceToken saved = deviceTokenRepository.findByFcmToken("fcm-1").orElseThrow();
		ReflectionTestUtils.setField(saved, "updatedAt", old);

		deviceTokenService.register(mom.getId(), new DeviceTokenRequest("fcm-1", Platform.ANDROID));

		assertThat(saved.getUpdatedAt()).isAfter(old);
	}

	@Test
	void registerMovesTokenToUserWhoRegisteredLast() {
		deviceTokenService.register(mom.getId(), new DeviceTokenRequest("fcm-1", Platform.ANDROID));
		User dad = saveUser("dad@example.com", "김아빠");

		deviceTokenService.register(dad.getId(), new DeviceTokenRequest("fcm-1", Platform.ANDROID));

		assertThat(deviceTokenRepository.findAll()).singleElement()
				.satisfies(saved -> assertThat(saved.getUser().getId()).isEqualTo(dad.getId()));
	}

	@Test
	void registerRejectsUnknownUser() {
		assertThatThrownBy(() -> deviceTokenService.register(999_999L,
				new DeviceTokenRequest("fcm-1", Platform.ANDROID)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
		assertThat(deviceTokenRepository.count()).isZero();
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
