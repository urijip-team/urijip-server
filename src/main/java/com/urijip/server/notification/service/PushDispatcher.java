package com.urijip.server.notification.service;

import java.util.List;
import java.util.Set;

import com.urijip.server.global.push.PushSender;
import com.urijip.server.member.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 푸시 요청을 트랜잭션이 커밋된 뒤 다른 스레드에서 보낸다. 롤백되면 보내지 않는다.
 * 트랜잭션 밖에서 요청해도 보낸다(fallbackExecution). 다시 쓸 수 없는 토큰은 지운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDispatcher {

	private final DeviceTokenService deviceTokenService;
	private final PushSender pushSender;

	@Async
	@TransactionalEventListener(fallbackExecution = true)
	public void onPushRequested(PushRequestedEvent event) {
		try {
			List<String> tokens = deviceTokenService.getTokens(event.userIds());
			if (tokens.isEmpty()) {
				return;
			}
			Set<String> invalid = pushSender.send(tokens, event.message());
			deviceTokenService.deleteTokens(invalid);
		} catch (RuntimeException e) {
			log.error("Push dispatch failed: type={}, users={}", event.message().type(), event.userIds().size(), e);
		}
	}

}
