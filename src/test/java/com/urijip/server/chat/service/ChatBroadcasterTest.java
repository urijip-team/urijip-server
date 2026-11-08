package com.urijip.server.chat.service;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;

import com.urijip.server.chat.dto.response.ChatMessageResponse;
import com.urijip.server.chat.dto.response.ChatReadResponse;
import com.urijip.server.chat.entity.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class ChatBroadcasterTest {

	private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);

	private final ChatBroadcaster broadcaster = new ChatBroadcaster(template);

	@Test
	void sendsNewMessageToFamilyChatTopic() {
		ChatMessageResponse message = new ChatMessageResponse(10L, MessageType.TEXT, 2L, "첫째", "안녕",
				OffsetDateTime.parse("2026-11-09T18:00:00+09:00"), 1);

		broadcaster.onMessageSent(new ChatMessageSentEvent(3L, message));

		then(template).should().convertAndSend("/sub/family/3/chat", (Object) message);
	}

	@Test
	void sendsReadProgressToFamilyReadTopic() {
		ChatReadResponse read = new ChatReadResponse(2L, 8L, 10L);

		broadcaster.onRead(new ChatReadEvent(3L, read));

		then(template).should().convertAndSend("/sub/family/3/read", (Object) read);
	}

}
