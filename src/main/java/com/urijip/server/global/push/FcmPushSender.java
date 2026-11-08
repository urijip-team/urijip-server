package com.urijip.server.global.push;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// FCM으로 보낸다. 한 번에 보낼 수 있는 토큰이 500개라 나눠 보낸다. 발송 실패는 로그만 남기고 예외를 던지지 않는다.
@Slf4j
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

	static final int MAX_TOKENS_PER_REQUEST = 500;

	// 이 오류가 난 토큰은 앱이 지워졌거나 다른 프로젝트의 것이라 다시 보내도 실패한다.
	private static final Set<MessagingErrorCode> INVALID_TOKEN_ERRORS = Set.of(MessagingErrorCode.UNREGISTERED,
			MessagingErrorCode.INVALID_ARGUMENT, MessagingErrorCode.SENDER_ID_MISMATCH);

	private final FirebaseMessaging firebaseMessaging;

	@Override
	public Set<String> send(Collection<String> tokens, PushMessage message) {
		// 같은 토큰은 한 번만 보낸다. 응답이 토큰 순서대로 오므로 순서를 지킨다.
		List<String> all = List.copyOf(new LinkedHashSet<>(tokens));
		Set<String> invalid = new HashSet<>();
		for (int from = 0; from < all.size(); from += MAX_TOKENS_PER_REQUEST) {
			List<String> chunk = all.subList(from, Math.min(from + MAX_TOKENS_PER_REQUEST, all.size()));
			invalid.addAll(sendChunk(chunk, message));
		}
		return invalid;
	}

	private List<String> sendChunk(List<String> tokens, PushMessage message) {
		try {
			BatchResponse response = firebaseMessaging.sendEachForMulticast(toMulticast(tokens, message));
			List<String> invalid = new ArrayList<>();
			List<SendResponse> responses = response.getResponses();
			for (int i = 0; i < responses.size(); i++) {
				FirebaseMessagingException error = responses.get(i).getException();
				if (error != null && INVALID_TOKEN_ERRORS.contains(error.getMessagingErrorCode())) {
					invalid.add(tokens.get(i));
				}
			}
			if (response.getFailureCount() > 0) {
				log.warn("Push partly failed: type={}, failed={}/{}, invalid={}", message.type(),
						response.getFailureCount(), tokens.size(), invalid.size());
			}
			return invalid;
		} catch (FirebaseMessagingException e) {
			log.error("Push failed: type={}, tokens={}, code={}", message.type(), tokens.size(),
					e.getMessagingErrorCode(), e);
			return List.of();
		}
	}

	// SOS는 기기가 잠들어 있어도 바로 받게 높은 우선순위로 보낸다.
	private MulticastMessage toMulticast(List<String> tokens, PushMessage message) {
		boolean urgent = message.type() == PushType.SOS;
		return MulticastMessage.builder()
				.addAllTokens(tokens)
				.setNotification(Notification.builder().setTitle(message.title()).setBody(message.body()).build())
				.putAllData(message.payload())
				.setAndroidConfig(AndroidConfig.builder()
						.setPriority(urgent ? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL)
						.build())
				.setApnsConfig(ApnsConfig.builder()
						.putHeader("apns-priority", urgent ? "10" : "5")
						.setAps(Aps.builder().setSound("default").build())
						.build())
				.build();
	}

}
