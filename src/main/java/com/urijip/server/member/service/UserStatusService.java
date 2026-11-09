package com.urijip.server.member.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.UserStatusChecker;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 필터와 STOMP 인터셉터가 요청마다 부른다. 요청마다 DB를 보지 않도록 상태를 CACHE_TTL 동안 기억한다.
 * 이 서버에서 정지·탈퇴시키면 바로 지우고(evict), 다른 서버에서 바뀐 것은 길어야 CACHE_TTL 뒤에 반영된다.
 */
@Service
@RequiredArgsConstructor
public class UserStatusService implements UserStatusChecker {

	static final Duration CACHE_TTL = Duration.ofSeconds(10);

	private final UserRepository userRepository;
	private final Map<Long, CachedStatus> cache = new ConcurrentHashMap<>();

	@Override
	@Transactional(readOnly = true)
	public void validateActive(Long userId) {
		UserStatus status = statusOf(userId);
		if (status == UserStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.USER_SUSPENDED);
		}
		if (status == UserStatus.WITHDRAWN) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
	}

	// 회원 상태를 바꾼 쪽이 부른다.
	public void evict(Long userId) {
		cache.remove(userId);
	}

	private UserStatus statusOf(Long userId) {
		long now = System.nanoTime();
		CachedStatus cached = cache.get(userId);
		if (cached != null && cached.expiresAt() - now > 0) {
			return cached.status();
		}
		UserStatus status = userRepository.findStatusById(userId).orElse(UserStatus.WITHDRAWN);
		cache.put(userId, new CachedStatus(status, now + CACHE_TTL.toNanos()));
		return status;
	}

	private record CachedStatus(UserStatus status, long expiresAt) {
	}

}
