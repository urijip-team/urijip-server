package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

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

	private static JwtProvider provider(String secret, Duration accessExpiration, Duration refreshExpiration) {
		return new JwtProvider(new JwtProperties(secret, accessExpiration, refreshExpiration));
	}

}
