package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

	private static final String EMAIL = "mom@example.com";

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-09T00:00:00Z"));

	private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

	@Test
	void fourFailuresStillAllowed() {
		fail(4);

		assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
	}

	@Test
	void fifthFailureLocksForTenMinutes() {
		fail(5);
		assertLocked(EMAIL);

		clock.advance(LoginAttemptLimiter.LOCK.minusSeconds(1));
		assertLocked(EMAIL);

		clock.advance(Duration.ofSeconds(1));
		assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
	}

	@Test
	void countStartsOverAfterLockEnds() {
		fail(5);
		clock.advance(LoginAttemptLimiter.LOCK);

		fail(1);

		assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
	}

	@Test
	void failuresOlderThanWindowAreForgotten() {
		fail(4);
		clock.advance(LoginAttemptLimiter.WINDOW);

		fail(1);

		assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
	}

	@Test
	void successResetsFailures() {
		fail(4);
		limiter.reset(EMAIL);

		fail(4);

		assertThatCode(() -> limiter.checkAllowed(EMAIL)).doesNotThrowAnyException();
	}

	@Test
	void emailIsComparedIgnoringCaseAndSpaces() {
		for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
			limiter.recordFailure(i % 2 == 0 ? " MOM@example.com " : EMAIL);
		}

		assertLocked("Mom@Example.com");
		assertThatCode(() -> limiter.checkAllowed("dad@example.com")).doesNotThrowAnyException();
	}

	private void fail(int times) {
		for (int i = 0; i < times; i++) {
			limiter.recordFailure(EMAIL);
		}
	}

	private void assertLocked(String email) {
		assertThatThrownBy(() -> limiter.checkAllowed(email))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS));
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

}
