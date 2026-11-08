package com.urijip.server.notification.service;

import java.util.Collection;
import java.util.Set;

import com.urijip.server.global.push.PushMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

// 다른 도메인이 푸시를 보낼 때 부른다. 바로 보내지 않고, 트랜잭션이 커밋된 뒤 PushDispatcher가 보낸다.
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final ApplicationEventPublisher eventPublisher;

	public void send(Collection<Long> userIds, PushMessage message) {
		Set<Long> receivers = Set.copyOf(userIds);
		if (receivers.isEmpty()) {
			return;
		}
		eventPublisher.publishEvent(new PushRequestedEvent(receivers, message));
	}

	// 활동 중인 모든 회원에게 보낸다. 받을 회원 id를 모으지 않고 PushDispatcher가 토큰을 한 번에 찾는다.
	public void sendToAll(PushMessage message) {
		eventPublisher.publishEvent(new BroadcastPushRequestedEvent(message));
	}

}
