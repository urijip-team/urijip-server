package com.urijip.server.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.chat.dto.request.ChatMessagePageRequest;
import com.urijip.server.chat.dto.response.ChatMessageResponse;
import com.urijip.server.chat.entity.MessageType;
import com.urijip.server.chat.service.ChatService;
import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.CursorResponse;
import com.urijip.server.global.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)
class ChatControllerTest {

	private static final ChatMessageResponse MESSAGE = new ChatMessageResponse(1024L, MessageType.TEXT, 2L, "첫째",
			"저녁 먹었어?", OffsetDateTime.parse("2026-10-02T18:30:00+09:00"));

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private ChatService chatService;

	@Test
	void getMessagesReturnsCursorPage() {
		given(chatService.getMessages(3L, 1L, new ChatMessagePageRequest(1100L, 20)))
				.willReturn(new CursorResponse<>(List.of(MESSAGE), 1024L, true));

		MvcTestResult result = mvc.get().uri("/api/families/3/messages?cursor=1100&size=20")
				.header("Authorization", bearer(1L)).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].messageId").isEqualTo(1024);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].type").isEqualTo("TEXT");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].senderId").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].senderName").isEqualTo("첫째");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].content").isEqualTo("저녁 먹었어?");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].createdAt")
				.isEqualTo("2026-10-02T18:30:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data.nextCursor").isEqualTo(1024);
		assertThat(result).bodyJson().extractingPath("$.data.hasNext").isEqualTo(true);
	}

	@Test
	void getMessagesWithoutParamsPassesNulls() {
		given(chatService.getMessages(3L, 1L, new ChatMessagePageRequest(null, null)))
				.willReturn(new CursorResponse<>(List.of(), null, false));

		MvcTestResult result = mvc.get().uri("/api/families/3/messages").header("Authorization", bearer(1L))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.hasNext").isEqualTo(false);
	}

	@Test
	void getMessagesWithInvalidCursorReturnsBadRequest() {
		MvcTestResult result = mvc.get().uri("/api/families/3/messages?cursor=abc")
				.header("Authorization", bearer(1L)).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
	}

	@Test
	void getMessagesOfOtherFamilyReturnsForbidden() {
		given(chatService.getMessages(any(), any(), any()))
				.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = mvc.get().uri("/api/families/3/messages").header("Authorization", bearer(9L))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void getMessagesWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/api/families/3/messages").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId, "USER");
	}

}
