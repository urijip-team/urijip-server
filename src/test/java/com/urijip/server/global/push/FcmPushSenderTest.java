package com.urijip.server.global.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.stream.IntStream;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.junit.jupiter.api.Test;

class FcmPushSenderTest {

	private static final PushMessage MESSAGE = PushMessage.of(PushType.SOS, "SOS", "첫째님이 SOS를 보냈습니다.");

	private final FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);

	private final FcmPushSender sender = new FcmPushSender(firebaseMessaging);

	@Test
	void successfulSendHasNoInvalidTokens() throws Exception {
		BatchResponse response = batch(success(), success());
		given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

		assertThat(sender.send(List.of("a", "b"), MESSAGE)).isEmpty();
	}

	@Test
	void returnsOnlyTokensThatCanNeverSucceed() throws Exception {
		BatchResponse response = batch(
				failure(MessagingErrorCode.UNREGISTERED), failure(MessagingErrorCode.UNAVAILABLE), success(),
				failure(MessagingErrorCode.INVALID_ARGUMENT), failure(MessagingErrorCode.SENDER_ID_MISMATCH));
		given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

		assertThat(sender.send(List.of("gone", "busy", "ok", "broken", "other-project"), MESSAGE))
				.containsExactlyInAnyOrder("gone", "broken", "other-project");
	}

	@Test
	void duplicateTokensAreSentOnce() throws Exception {
		BatchResponse response = batch(success(), failure(MessagingErrorCode.UNREGISTERED));
		given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

		assertThat(sender.send(List.of("a", "a", "b"), MESSAGE)).containsExactly("b");
	}

	@Test
	void splitsIntoRequestsOfAtMost500Tokens() throws Exception {
		BatchResponse response = batch();
		given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);
		List<String> tokens = IntStream.range(0, 1001).mapToObj(i -> "token-" + i).toList();

		sender.send(tokens, MESSAGE);

		verify(firebaseMessaging, times(3)).sendEachForMulticast(any(MulticastMessage.class));
	}

	@Test
	void sendsNothingForNoTokens() throws Exception {
		assertThat(sender.send(List.of(), MESSAGE)).isEmpty();

		verify(firebaseMessaging, never()).sendEachForMulticast(any(MulticastMessage.class));
	}

	@Test
	void requestFailureIsLoggedNotThrown() throws Exception {
		FirebaseMessagingException error = mock(FirebaseMessagingException.class);
		given(error.getMessagingErrorCode()).willReturn(MessagingErrorCode.INTERNAL);
		given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willThrow(error);

		assertThat(sender.send(List.of("a", "b"), MESSAGE)).isEmpty();
	}

	private static BatchResponse batch(SendResponse... responses) {
		int failures = (int) List.of(responses).stream().filter(r -> !r.isSuccessful()).count();
		BatchResponse batch = mock(BatchResponse.class);
		given(batch.getResponses()).willReturn(List.of(responses));
		given(batch.getFailureCount()).willReturn(failures);
		return batch;
	}

	private static SendResponse success() {
		SendResponse response = mock(SendResponse.class);
		given(response.isSuccessful()).willReturn(true);
		return response;
	}

	private static SendResponse failure(MessagingErrorCode code) {
		FirebaseMessagingException error = mock(FirebaseMessagingException.class);
		given(error.getMessagingErrorCode()).willReturn(code);
		SendResponse response = mock(SendResponse.class);
		given(response.isSuccessful()).willReturn(false);
		given(response.getException()).willReturn(error);
		return response;
	}

}
