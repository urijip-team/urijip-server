package com.urijip.server.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.member.entity.User;
import org.junit.jupiter.api.Test;

class NotificationSettingTest {

	private final User mom = User.builder().email("mom@example.com").password("pw").name("김엄마").build();

	@Test
	void updateChangesOnlySentValues() {
		NotificationSetting setting = new NotificationSetting(mom);

		setting.update(false, null, null, false, null);

		assertThat(setting.isChat()).isFalse();
		assertThat(setting.isSchedule()).isTrue();
		assertThat(setting.isAllowance()).isTrue();
		assertThat(setting.isNotice()).isFalse();

		setting.update(true, false, false, null, null);

		assertThat(setting.isChat()).isTrue();
		assertThat(setting.isSchedule()).isFalse();
		assertThat(setting.isAllowance()).isFalse();
		assertThat(setting.isNotice()).isFalse();
	}

	@Test
	void placeAlertStartsOnAndCanBeTurnedOff() {
		NotificationSetting setting = new NotificationSetting(mom);
		assertThat(setting.isPlace()).isTrue();

		setting.update(null, null, null, null, false);

		assertThat(setting.isPlace()).isFalse();
		assertThat(setting.isChat()).isTrue();
	}

}
