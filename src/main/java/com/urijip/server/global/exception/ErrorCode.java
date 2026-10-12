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
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
	FAMILY_NOT_MEMBER(HttpStatus.FORBIDDEN, "해당 가족의 멤버가 아닙니다"),
	NOT_FAMILY_OWNER(HttpStatus.FORBIDDEN, "방장 권한이 필요합니다"),
	USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다"),
	FAMILY_NOT_FOUND(HttpStatus.NOT_FOUND, "참여한 가족이 없습니다"),
	FAMILY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 가족의 멤버를 찾을 수 없습니다"),
	INVITE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 초대 코드입니다"),
	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다"),
	OWNER_CANNOT_LEAVE(HttpStatus.CONFLICT, "방장은 방장을 위임한 뒤에 나갈 수 있습니다"),
	ALREADY_FAMILY_MEMBER(HttpStatus.CONFLICT, "이미 가족에 참여하고 있습니다"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

	private final HttpStatus status;
	private final String message;

}
