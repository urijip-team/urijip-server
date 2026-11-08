package com.urijip.server.global.push;

import java.util.Collection;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

// Firebase 키가 없을 때(로컬, 테스트) 쓰는 발송기. 실제로 보내지 않고 로그만 남긴다.
@Slf4j
public class LoggingPushSender implements PushSender {

	@Override
	public Set<String> send(Collection<String> tokens, PushMessage message) {
		log.info("Push skipped (FCM not configured): type={}, title={}, tokens={}", message.type(), message.title(),
				tokens.size());
		return Set.of();
	}

}
