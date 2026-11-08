package com.urijip.server.notification.dto.response;

import com.urijip.server.notification.entity.NotificationSetting;

public record NotificationSettingResponse(boolean chat, boolean schedule, boolean allowance, boolean notice) {

	// 설정을 저장한 적 없는 회원은 모두 켜져 있다.
	public static final NotificationSettingResponse ALL_ON = new NotificationSettingResponse(true, true, true, true);

	public static NotificationSettingResponse from(NotificationSetting setting) {
		return new NotificationSettingResponse(setting.isChat(), setting.isSchedule(), setting.isAllowance(),
				setting.isNotice());
	}

}
