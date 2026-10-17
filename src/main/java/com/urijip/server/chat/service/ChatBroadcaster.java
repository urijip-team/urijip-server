package com.urijip.server.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// 저장이 커밋된 뒤에 가족에게 보낸다. 롤백된 메시지가 채팅방에 나타나지 않게 하기 위해서다.
@Component
@RequiredArgsConstructor
public class ChatBroadcaster {

	private final SimpMessagingTemplate messagingTemplate;

	@TransactionalEventListener
	public void onMessageSent(ChatMessageSentEvent event) {
		messagingTemplate.convertAndSend("/sub/family/" + event.familyId() + "/chat", event.message());
	}

}
