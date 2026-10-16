package com.urijip.server.family.service;

/**
 * 가족이 삭제되기 직전에 같은 트랜잭션 안에서 발행된다. 가족에 딸린 데이터를 가진 도메인은 이 이벤트를 받아 자기 데이터를 지운다.
 * {@code family}가 다른 도메인의 서비스를 직접 부르면 순환 참조가 생기므로 이벤트로 알린다.
 */
public record FamilyDeletedEvent(Long familyId) {
}
