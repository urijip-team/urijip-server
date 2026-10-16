package com.urijip.server.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * STOMP {@code @MessageMapping} 처리 중 난 예외. 보낸 앱에 따로 응답하지 않고 메시지를 버린다.
 * 처리하지 않으면 잘못된 메시지마다 ERROR 로그와 스택 트레이스가 남는다.
 */
@Slf4j
@ControllerAdvice
public class StompExceptionHandler {

	@MessageExceptionHandler(BusinessException.class)
	public void handleBusiness(BusinessException ex) {
		log.warn("STOMP business exception: {}", ex.getErrorCode());
	}

	@MessageExceptionHandler(MethodArgumentNotValidException.class)
	public void handleInvalidPayload(MethodArgumentNotValidException ex) {
		log.warn("STOMP invalid payload: {}", ex.getBindingResult());
	}

}
