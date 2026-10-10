package com.urijip.server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
	USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다"),
	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

	private final HttpStatus status;
	private final String message;

}
