package com.urijip.server.notification.service;

import java.util.Set;

import com.urijip.server.global.push.PushMessage;

public record PushRequestedEvent(Set<Long> userIds, PushMessage message) {

	public PushRequestedEvent {
		userIds = Set.copyOf(userIds);
	}

}
