package com.urijip.server.member.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 같은 이메일로 WINDOW 안에 MAX_FAILURES번 틀리면 LOCK 동안 로그인을 막는다(비밀번호 대입 방지).
 * 막힌 동안은 맞는 비밀번호도 거절하고, 성공하면 기록을 지운다. 없는 이메일도 같은 규칙이라 가입 여부가 드러나지 않는다.
 * 서버 메모리에 두므로 서버를 여러 대 띄우면 서버마다 따로 센다.
 */
@Component
public class LoginAttemptLimiter {

	static final int MAX_FAILURES = 5;

	static final Duration WINDOW = Duration.ofMinutes(10);

	static final Duration LOCK = Duration.ofMinutes(10);

	// 기록이 이만큼 쌓이면 끝난 기록을 정리한다.
	private static final int CLEANUP_THRESHOLD = 10_000;

	private final Clock clock;
	private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

	public LoginAttemptLimiter() {
		this(Clock.systemUTC());
	}

	LoginAttemptLimiter(Clock clock) {
		this.clock = clock;
	}

	// 막혀 있으면 TOO_MANY_LOGIN_ATTEMPTS.
	public void checkAllowed(String email) {
		Attempts current = attempts.get(key(email));
		if (current != null && current.lockedUntil() != null && clock.instant().isBefore(current.lockedUntil())) {
			throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
		}
	}

	public void recordFailure(String email) {
		Instant now = clock.instant();
		attempts.compute(key(email), (k, current) -> {
			Attempts fresh = current == null || current.isOver(now) ? new Attempts(0, now, null) : current;
			int failures = fresh.failures() + 1;
			Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK) : null;
			return new Attempts(failures, fresh.windowStart(), lockedUntil);
		});
		if (attempts.size() > CLEANUP_THRESHOLD) {
			attempts.values().removeIf(entry -> entry.isOver(now));
		}
	}

	public void reset(String email) {
		attempts.remove(key(email));
	}

	private String key(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	private record Attempts(int failures, Instant windowStart, Instant lockedUntil) {

		// 잠금이 풀렸거나, 잠기지 않은 채 WINDOW가 지났으면 처음부터 센다.
		boolean isOver(Instant now) {
			if (lockedUntil != null) {
				return !now.isBefore(lockedUntil);
			}
			return !now.isBefore(windowStart.plus(WINDOW));
		}

	}

}
