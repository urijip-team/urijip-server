package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;

class StompErrorHandlerTest {

	private final StompErrorHandler errorHandler = new StompErrorHandler();

	@Test
	void businessExceptionBecomesErrorFrameWithCode() {
		Message<byte[]> error = errorHandler.handleClientMessageProcessingError(null,
				new IllegalStateException("wrapped", new BusinessException(ErrorCode.TOKEN_EXPIRED)));

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(error, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
		assertThat(accessor.getMessage()).isEqualTo("TOKEN_EXPIRED");
		assertThat(new String(error.getPayload(), StandardCharsets.UTF_8)).isEqualTo("토큰이 만료되었습니다");
	}

	@Test
	void unexpectedExceptionHidesDetails() {
		Message<byte[]> error = errorHandler.handleClientMessageProcessingError(null,
				new IllegalStateException("db password is wrong"));

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(error, StompHeaderAccessor.class);
		assertThat(accessor.getMessage()).isEqualTo("INTERNAL_SERVER_ERROR");
		assertThat(new String(error.getPayload(), StandardCharsets.UTF_8)).doesNotContain("password");
	}

}
