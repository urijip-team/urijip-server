package com.urijip.server.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notification.entity.NotificationSetting;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class NotificationSettingRepositoryTest {

	@Autowired
	private NotificationSettingRepository notificationSettingRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void savedSettingUsesUserIdAsKeyAndStartsAllOn() {
		User mom = saveUser("mom@example.com");

		notificationSettingRepository.saveAndFlush(new NotificationSetting(mom));
		entityManager.clear();

		NotificationSetting saved = notificationSettingRepository.findById(mom.getId()).orElseThrow();
		assertThat(saved.isChat()).isTrue();
		assertThat(saved.isSchedule()).isTrue();
		assertThat(saved.isAllowance()).isTrue();
		assertThat(saved.isNotice()).isTrue();
	}

	@Test
	void findUserIdsWithNoticeOffReturnsOnlyThoseUsers() {
		User mom = saveUser("mom@example.com");
		User dad = saveUser("dad@example.com");
		NotificationSetting off = new NotificationSetting(mom);
		off.update(true, true, true, false, null);
		notificationSettingRepository.save(off);
		NotificationSetting chatOff = new NotificationSetting(dad);
		chatOff.update(false, true, true, true, null);
		notificationSettingRepository.saveAndFlush(chatOff);

		assertThat(notificationSettingRepository.findUserIdsWithNoticeOff()).containsExactly(mom.getId());
	}

	private User saveUser(String email) {
		return userRepository.save(User.builder().email(email).password("pw").name("회원").build());
	}

}
