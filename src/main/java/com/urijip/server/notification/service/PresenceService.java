package com.urijip.server.notification.service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

/**
 * 앱이 켜져 있는지(WebSocket에 연결돼 있는지) 본다. STOMP CONNECT 때 넣은 Principal의 이름이 회원 id다.
 * 앱이 꺼지거나 연결이 끊기면 하트비트(10초)가 끊긴 뒤 연결 목록에서 빠진다. 서버를 여러 대 띄우면 이 서버의 연결만 본다.
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

	private final SimpUserRegistry simpUserRegistry;

	public Set<Long> filterOffline(Collection<Long> userIds) {
		return userIds.stream()
				.filter(userId -> simpUserRegistry.getUser(userId.toString()) == null)
				.collect(Collectors.toSet());
	}

}
