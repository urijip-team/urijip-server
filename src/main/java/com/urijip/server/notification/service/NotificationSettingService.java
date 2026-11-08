package com.urijip.server.notification.service;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.urijip.server.global.push.PushType;
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

	// 이 종류의 푸시를 끈 회원을 뺀다. SOS와 문의 답변은 끌 수 없어 모두 받는다.
	@Transactional(readOnly = true)
	public Set<Long> filterReceivers(Collection<Long> userIds, PushType type) {
		Predicate<NotificationSetting> enabled = enabledCheck(type);
		if (enabled == null || userIds.isEmpty()) {
			return Set.copyOf(userIds);
		}
		Set<Long> disabled = notificationSettingRepository.findAllById(userIds).stream()
				.filter(enabled.negate())
				.map(NotificationSetting::getUserId)
				.collect(Collectors.toSet());
		return userIds.stream().filter(id -> !disabled.contains(id)).collect(Collectors.toSet());
	}

	// 설정으로 끌 수 있는 종류면 켜져 있는지 보는 조건, 끌 수 없으면 null.
	private Predicate<NotificationSetting> enabledCheck(PushType type) {
		return switch (type) {
			case CHAT -> NotificationSetting::isChat;
			case SCHEDULE -> NotificationSetting::isSchedule;
			case ALLOWANCE -> NotificationSetting::isAllowance;
			case NOTICE -> NotificationSetting::isNotice;
			case SOS, INQUIRY -> null;
		};
	}

}
