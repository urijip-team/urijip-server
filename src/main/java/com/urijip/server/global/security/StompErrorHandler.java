package com.urijip.server.global.security;

import java.nio.charset.StandardCharsets;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * 인터셉터에서 난 {@link BusinessException}을 ERROR 프레임으로 바꾼다. {@code message} 헤더에 에러 코드
 * (예: {@code TOKEN_EXPIRED})를, 본문에 한국어 메시지를 넣는다. 앱은 헤더를 보고 재발급 여부를 정한다.
 */
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

	@Override
	public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
		ErrorCode errorCode = findErrorCode(ex);
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
		accessor.setMessage(errorCode.name());
		accessor.setLeaveMutable(true);

		StompHeaderAccessor clientHeaderAccessor = clientMessage == null ? null
				: MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
		return handleInternal(accessor, errorCode.getMessage().getBytes(StandardCharsets.UTF_8), ex,
				clientHeaderAccessor);
	}

	private ErrorCode findErrorCode(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof BusinessException businessException) {
				return businessException.getErrorCode();
			}
		}
		return ErrorCode.INTERNAL_SERVER_ERROR;
	}

}
