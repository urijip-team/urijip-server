package com.urijip.server.global.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PushMessageTest {

	@Test
	void payloadIncludesTypeAndData() {
		PushMessage message = new PushMessage(PushType.ALLOWANCE, "용돈 요청", "첫째님이 용돈을 요청했어요.",
				Map.of("requestId", "45"));

		assertThat(message.payload()).containsOnly(Map.entry("type", "ALLOWANCE"), Map.entry("requestId", "45"));
	}

	@Test
	void ofHasOnlyType() {
		assertThat(PushMessage.of(PushType.NOTICE, "공지", "점검 안내").payload())
				.containsOnly(Map.entry("type", "NOTICE"));
	}

	@Test
	void dataIsCopied() {
		Map<String, String> data = new HashMap<>(Map.of("a", "1"));
		PushMessage message = new PushMessage(PushType.CHAT, "t", "b", data);

		data.put("b", "2");

		assertThat(message.data()).containsOnly(Map.entry("a", "1"));
	}

	@Test
	void typeIsRequired() {
		assertThatThrownBy(() -> PushMessage.of(null, "t", "b")).isInstanceOf(NullPointerException.class);
	}

}
