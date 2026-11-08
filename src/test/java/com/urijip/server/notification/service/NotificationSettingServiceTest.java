package com.urijip.server.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.push.PushType;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notification.dto.request.NotificationSettingUpdateRequest;
import com.urijip.server.notification.dto.response.NotificationSettingResponse;
import com.urijip.server.notification.entity.NotificationSetting;
import com.urijip.server.notification.repository.NotificationSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

	@Test
	void firstUpdateCreatesSettingWithOnlySentValuesOff() {
		NotificationSettingResponse response = notificationSettingService.update(mom.getId(),
				new NotificationSettingUpdateRequest(false, null, null, null));

		assertThat(response).isEqualTo(new NotificationSettingResponse(false, true, true, true));
		assertThat(notificationSettingRepository.findById(mom.getId())).isPresent();
		assertThat(notificationSettingRepository.findById(dad.getId())).isEmpty();
	}

	@Test
	void updateKeepsValuesThatWereNotSent() {
		saveSetting(mom, false, true, false, true);

		NotificationSettingResponse response = notificationSettingService.update(mom.getId(),
				new NotificationSettingUpdateRequest(null, false, true, null));

		assertThat(response).isEqualTo(new NotificationSettingResponse(false, false, true, true));
		assertThat(notificationSettingService.getSettings(mom.getId())).isEqualTo(response);
	}

	@Test
	void updateOfUnknownUserFails() {
		assertThatThrownBy(() -> notificationSettingService.update(-1L,
				new NotificationSettingUpdateRequest(false, null, null, null)))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
	}

	@Test
	void filterReceiversDropsUsersWhoTurnedTypeOff() {
		saveSetting(mom, false, true, true, true);

		assertThat(notificationSettingService.filterReceivers(List.of(mom.getId(), dad.getId()), PushType.CHAT))
				.isEqualTo(Set.of(dad.getId()));
		assertThat(notificationSettingService.filterReceivers(List.of(mom.getId(), dad.getId()), PushType.SCHEDULE))
				.isEqualTo(Set.of(mom.getId(), dad.getId()));
	}

	@Test
	void filterReceiversChecksEachTypeAgainstItsSetting() {
		saveSetting(mom, true, false, false, false);
		List<Long> receivers = List.of(mom.getId());

		assertThat(notificationSettingService.filterReceivers(receivers, PushType.CHAT)).containsExactly(mom.getId());
		assertThat(notificationSettingService.filterReceivers(receivers, PushType.SCHEDULE)).isEmpty();
		assertThat(notificationSettingService.filterReceivers(receivers, PushType.ALLOWANCE)).isEmpty();
		assertThat(notificationSettingService.filterReceivers(receivers, PushType.NOTICE)).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(value = PushType.class, names = {"SOS", "INQUIRY"})
	void filterReceiversKeepsEveryoneForTypesThatCannotBeTurnedOff(PushType type) {
		saveSetting(mom, false, false, false, false);

		assertThat(notificationSettingService.filterReceivers(List.of(mom.getId()), type))
				.containsExactly(mom.getId());
	}

	@Test
	void filterNoReceiversIsEmpty() {
		assertThat(notificationSettingService.filterReceivers(List.of(), PushType.CHAT)).isEmpty();
	}

	@Test
	void getNoticeOffUserIdsReturnsUsersWhoTurnedNoticeOff() {
		saveSetting(mom, true, true, true, false);
		saveSetting(dad, false, false, false, true);

		assertThat(notificationSettingService.getNoticeOffUserIds()).containsExactly(mom.getId());
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
