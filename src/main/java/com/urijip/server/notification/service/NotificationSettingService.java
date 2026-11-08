package com.urijip.server.notification.service;

import com.urijip.server.notification.dto.response.NotificationSettingResponse;
import com.urijip.server.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

	private final NotificationSettingRepository notificationSettingRepository;

	@Transactional(readOnly = true)
	public NotificationSettingResponse getSettings(Long userId) {
		return notificationSettingRepository.findById(userId)
				.map(NotificationSettingResponse::from)
				.orElse(NotificationSettingResponse.ALL_ON);
	}

}
