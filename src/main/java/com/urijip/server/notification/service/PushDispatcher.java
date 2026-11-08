package com.urijip.server.notification.service;

import java.util.List;
import java.util.Set;

import com.urijip.server.global.push.PushMessage;
import com.urijip.server.global.push.PushSender;
import com.urijip.server.member.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 푸시 요청을 트랜잭션이 커밋된 뒤 다른 스레드에서 보낸다. 롤백되면 보내지 않는다.
 * 트랜잭션 밖에서 요청해도 보낸다(fallbackExecution). 그 종류의 알림을 끈 회원에게는 보내지 않고, 다시 쓸 수 없는 토큰은 지운다.
 * 전체 푸시는 활동 중인 회원의 토큰을 모두 모아 보낸다. FCM 발송기가 500개씩 나눠 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDispatcher {

	private final DeviceTokenService deviceTokenService;
	private final PushSender pushSender;
	private final NotificationSettingService notificationSettingService;

	@Async
	@TransactionalEventListener(fallbackExecution = true)
	public void onPushRequested(PushRequestedEvent event) {
		try {
			Set<Long> receivers = notificationSettingService.filterReceivers(event.userIds(), event.message().type());
			dispatch(deviceTokenService.getTokens(receivers), event.message());
		} catch (RuntimeException e) {
			log.error("Push dispatch failed: type={}, users={}", event.message().type(), event.userIds().size(), e);
		}
	}

	@Async
	@TransactionalEventListener(fallbackExecution = true)
	public void onBroadcastRequested(BroadcastPushRequestedEvent event) {
		try {
			dispatch(deviceTokenService.getActiveUserTokens(), event.message());
		} catch (RuntimeException e) {
			log.error("Broadcast push dispatch failed: type={}", event.message().type(), e);
		}
	}

	private void dispatch(List<String> tokens, PushMessage message) {
		if (tokens.isEmpty()) {
			return;
		}
		Set<String> invalid = pushSender.send(tokens, message);
		deviceTokenService.deleteTokens(invalid);
	}

}
