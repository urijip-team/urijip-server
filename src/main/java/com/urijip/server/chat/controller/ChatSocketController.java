package com.urijip.server.chat.controller;

import java.security.Principal;

import com.urijip.server.chat.dto.request.ChatSendRequest;
import com.urijip.server.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

	private final ChatService chatService;

	// principal 이름은 CONNECT에서 정한 회원 id다(StompAuthChannelInterceptor).
	@MessageMapping("/chat")
	public void send(Principal principal, @Valid @Payload ChatSendRequest request) {
		chatService.send(Long.valueOf(principal.getName()), request);
	}

}
