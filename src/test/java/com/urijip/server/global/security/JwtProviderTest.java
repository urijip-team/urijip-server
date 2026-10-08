package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtProviderTest {

	private static final String SECRET = "test-secret-key-that-is-long-enough-0123456789";

	private final JwtProvider jwtProvider = provider(SECRET, Duration.ofMinutes(30), Duration.ofDays(14));

	@Test
	void accessTokenCarriesUserIdAndRole() {
		String token = jwtProvider.createAccessToken(1L, "USER");

		AccessTokenClaims claims = jwtProvider.parseAccessToken(token);

		assertThat(claims.userId()).isEqualTo(1L);
		assertThat(claims.role()).isEqualTo("USER");
	}

	@Test
	void refreshTokenCarriesUserId() {
		String token = jwtProvider.createRefreshToken(1L);

		assertThat(jwtProvider.parseRefreshToken(token)).isEqualTo(1L);
	}

	@Test
	void refreshTokensDifferEvenForSameUser() {
		String first = jwtProvider.createRefreshToken(1L);
		String second = jwtProvider.createRefreshToken(1L);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void expiredAccessTokenIsRejectedAsTokenExpired() {
		JwtProvider expired = provider(SECRET, Duration.ofSeconds(-1), Duration.ofDays(14));
		String token = expired.createAccessToken(1L, "USER");

		assertErrorCode(() -> jwtProvider.parseAccessToken(token), ErrorCode.TOKEN_EXPIRED);
	}

	@Test
	void expiredRefreshTokenIsRejectedAsTokenExpired() {
		JwtProvider expired = provider(SECRET, Duration.ofMinutes(30), Duration.ofSeconds(-1));
		String token = expired.createRefreshToken(1L);

		assertErrorCode(() -> jwtProvider.parseRefreshToken(token), ErrorCode.TOKEN_EXPIRED);
	}

	@Test
	void tokenSignedWithAnotherKeyIsInvalid() {
		JwtProvider other = provider("another-secret-key-that-is-long-enough-9876543210",
				Duration.ofMinutes(30), Duration.ofDays(14));
		String token = other.createAccessToken(1L, "ADMIN");

		assertErrorCode(() -> jwtProvider.parseAccessToken(token), ErrorCode.INVALID_TOKEN);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"not-a-token", "a.b.c"})
	void malformedTokenIsInvalid(String token) {
		assertErrorCode(() -> jwtProvider.parseAccessToken(token), ErrorCode.INVALID_TOKEN);
	}

	@Test
	void refreshTokenCannotBeUsedAsAccessToken() {
		String token = jwtProvider.createRefreshToken(1L);

		assertErrorCode(() -> jwtProvider.parseAccessToken(token), ErrorCode.INVALID_TOKEN);
	}

	@Test
	void accessTokenCannotBeUsedAsRefreshToken() {
		String token = jwtProvider.createAccessToken(1L, "USER");

		assertErrorCode(() -> jwtProvider.parseRefreshToken(token), ErrorCode.INVALID_TOKEN);
	}

	private static JwtProvider provider(String secret, Duration accessExpiration, Duration refreshExpiration) {
		return new JwtProvider(new JwtProperties(secret, accessExpiration, refreshExpiration));
	}

	private static void assertErrorCode(ThrowingCallable call, ErrorCode expected) {
		assertThatThrownBy(call)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(expected);
	}

}
