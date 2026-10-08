package com.urijip.server.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

	private static final String TYPE_CLAIM = "type";
	private static final String ROLE_CLAIM = "role";
	private static final String ACCESS = "access";

	private final SecretKey key;
	private final Duration accessExpiration;

	public JwtProvider(JwtProperties properties) {
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.accessExpiration = properties.accessExpiration();
	}

	public String createAccessToken(Long userId, String role) {
		return builder(userId, ACCESS, accessExpiration).claim(ROLE_CLAIM, role).compact();
	}

	private JwtBuilder builder(Long userId, String type, Duration expiration) {
		Instant now = Instant.now();
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(String.valueOf(userId))
				.claim(TYPE_CLAIM, type)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expiration)))
				.signWith(key);
	}

}
