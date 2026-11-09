package com.urijip.server.global.security;

/**
 * 토큰이 유효해도 정지·탈퇴한 회원은 막기 위한 확인. {@code global}은 도메인에 의존하지 않으므로 인터페이스만 두고
 * 회원 도메인이 구현한다.
 */
public interface UserStatusChecker {

	/**
	 * 정지된 회원이면 {@code USER_SUSPENDED}, 탈퇴했거나 없는 회원이면 {@code INVALID_TOKEN}으로
	 * {@link com.urijip.server.global.exception.BusinessException}을 던진다.
	 */
	void validateActive(Long userId);

}
