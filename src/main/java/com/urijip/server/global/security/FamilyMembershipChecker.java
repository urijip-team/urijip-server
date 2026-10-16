package com.urijip.server.global.security;

/**
 * STOMP 구독 검사에서 쓰는 가족 멤버 확인. {@code global}은 도메인에 의존하지 않으므로 인터페이스만 두고
 * 가족 도메인이 구현한다.
 */
public interface FamilyMembershipChecker {

	/** 멤버가 아니면 {@code FAMILY_NOT_MEMBER}로 {@link com.urijip.server.global.exception.BusinessException}을 던진다. */
	void validateMember(Long familyId, Long userId);

}
