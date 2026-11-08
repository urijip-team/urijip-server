package com.urijip.server.global.push;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 기기에 보낼 푸시 한 건. title·body는 알림에 보이는 글이고, data는 앱이 화면을 고르는 데 쓰는 값이다.
 * FCM data는 문자열만 담을 수 있어 Map<String, String>이다.
 */
public record PushMessage(PushType type, String title, String body, Map<String, String> data) {

	public PushMessage {
		Objects.requireNonNull(type, "type");
		data = data == null ? Map.of() : Map.copyOf(data);
	}

	public static PushMessage of(PushType type, String title, String body) {
		return new PushMessage(type, title, body, Map.of());
	}

	// 앱이 받는 data. type을 함께 넣는다.
	public Map<String, String> payload() {
		Map<String, String> payload = new LinkedHashMap<>(data);
		payload.put("type", type.name());
		return payload;
	}

}
