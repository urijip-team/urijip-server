package com.urijip.server.chat.controller;

import com.urijip.server.chat.dto.request.ChatMessagePageRequest;
import com.urijip.server.chat.dto.response.ChatMessageResponse;
import com.urijip.server.chat.service.ChatService;
import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.CursorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	// multipart의 image 파트에 사진 한 장. 저장한 메시지는 가족에게 실시간으로도 간다.
	@PostMapping(path = "/api/families/{familyId}/messages/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ChatMessageResponse> sendImage(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId, @RequestPart("image") MultipartFile image) {
		return ApiResponse.success(chatService.sendImage(familyId, userId, image));
	}

	@GetMapping("/api/families/{familyId}/messages")
	public ApiResponse<CursorResponse<ChatMessageResponse>> getMessages(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId, @ModelAttribute ChatMessagePageRequest request) {
		return ApiResponse.success(chatService.getMessages(familyId, userId, request));
	}

}
