package com.urijip.server.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notification.dto.response.NotificationSettingResponse;
import com.urijip.server.notification.entity.NotificationSetting;
import com.urijip.server.notification.repository.NotificationSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class NotificationSettingServiceTest {

	@Autowired
	private NotificationSettingService notificationSettingService;

	@Autowired
	private NotificationSettingRepository notificationSettingRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User dad;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		dad = saveUser("dad@example.com", "김아빠");
	}

	@Test
	void getSettingsWithoutSavedRowIsAllOn() {
		assertThat(notificationSettingService.getSettings(mom.getId())).isEqualTo(NotificationSettingResponse.ALL_ON);
	}

	@Test
	void getSettingsReturnsSavedValues() {
		saveSetting(mom, false, true, false, true);

		assertThat(notificationSettingService.getSettings(mom.getId()))
				.isEqualTo(new NotificationSettingResponse(false, true, false, true));
	}

	private void saveSetting(User user, boolean chat, boolean schedule, boolean allowance, boolean notice) {
		NotificationSetting setting = new NotificationSetting(user);
		setting.update(chat, schedule, allowance, notice);
		notificationSettingRepository.save(setting);
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder().email(email).password("encoded-password").name(name).build());
	}

}
