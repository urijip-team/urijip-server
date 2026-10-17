package com.urijip.server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
	INVALID_IMAGE(HttpStatus.BAD_REQUEST, "JPEG 또는 PNG 이미지만 올릴 수 있습니다"),
	TOO_MANY_PHOTOS(HttpStatus.BAD_REQUEST, "사진은 한 번에 10장까지 올릴 수 있습니다"),
	CANNOT_REPORT_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 신고할 수 없습니다"),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
	FAMILY_NOT_MEMBER(HttpStatus.FORBIDDEN, "해당 가족의 멤버가 아닙니다"),
	NOT_FAMILY_OWNER(HttpStatus.FORBIDDEN, "방장 권한이 필요합니다"),
	NOT_PHOTO_UPLOADER(HttpStatus.FORBIDDEN, "사진을 올린 사람만 삭제할 수 있습니다"),
	NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "댓글을 쓴 사람만 삭제할 수 있습니다"),
	USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다"),
	FAMILY_NOT_FOUND(HttpStatus.NOT_FOUND, "참여한 가족이 없습니다"),
	FAMILY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 가족의 멤버를 찾을 수 없습니다"),
	INVITE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 초대 코드입니다"),
	SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다"),
	TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 할 일입니다"),
	ALBUM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 앨범입니다"),
	PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사진입니다"),
	COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다"),
	REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 신고입니다"),
	PAYMENT_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 송금 정보가 없습니다"),
	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다"),
	USER_WITHDRAWN(HttpStatus.CONFLICT, "탈퇴한 회원입니다"),
	OWNER_CANNOT_LEAVE(HttpStatus.CONFLICT, "방장은 방장을 위임한 뒤에 나갈 수 있습니다"),
	ALREADY_FAMILY_MEMBER(HttpStatus.CONFLICT, "이미 가족에 참여하고 있습니다"),
	ALREADY_REPORTED(HttpStatus.CONFLICT, "이미 신고해서 처리를 기다리는 대상입니다"),
	REPORT_ALREADY_HANDLED(HttpStatus.CONFLICT, "이미 처리된 신고입니다"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

	private final HttpStatus status;
	private final String message;

}
