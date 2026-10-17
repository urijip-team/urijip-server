package com.urijip.server.chat.controller;

import com.urijip.server.chat.dto.request.ChatMessagePageRequest;
import com.urijip.server.chat.dto.response.ChatMessageResponse;
import com.urijip.server.chat.service.ChatService;
import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.CursorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@GetMapping("/api/families/{familyId}/messages")
	public ApiResponse<CursorResponse<ChatMessageResponse>> getMessages(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId, @ModelAttribute ChatMessagePageRequest request) {
		return ApiResponse.success(chatService.getMessages(familyId, userId, request));
	}

}
