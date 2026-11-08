package com.urijip.server.notification.service;

import com.urijip.server.member.service.UserService;
import com.urijip.server.notification.dto.request.NotificationSettingUpdateRequest;
import com.urijip.server.notification.dto.response.NotificationSettingResponse;
import com.urijip.server.notification.entity.NotificationSetting;
import com.urijip.server.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

	private final NotificationSettingRepository notificationSettingRepository;
	private final UserService userService;

	@Transactional(readOnly = true)
	public NotificationSettingResponse getSettings(Long userId) {
		return notificationSettingRepository.findById(userId)
				.map(NotificationSettingResponse::from)
				.orElse(NotificationSettingResponse.ALL_ON);
	}

	// 처음 바꾸면 모두 켜진 설정을 만든 뒤 바꾼다.
	@Transactional
	public NotificationSettingResponse update(Long userId, NotificationSettingUpdateRequest request) {
		NotificationSetting setting = notificationSettingRepository.findById(userId)
				.orElseGet(() -> notificationSettingRepository.save(new NotificationSetting(userService.getUser(userId))));
		setting.update(request.chat(), request.schedule(), request.allowance(), request.notice());
		return NotificationSettingResponse.from(setting);
	}

}
