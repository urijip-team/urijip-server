package com.urijip.server.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

	private static final String TYPE_CLAIM = "type";
	private static final String ROLE_CLAIM = "role";
	private static final String ACCESS = "access";
	private static final String REFRESH = "refresh";

	private final SecretKey key;
	private final Duration accessExpiration;
	private final Duration refreshExpiration;

	public JwtProvider(JwtProperties properties) {
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.accessExpiration = properties.accessExpiration();
		this.refreshExpiration = properties.refreshExpiration();
	}

	public String createAccessToken(Long userId, String role) {
		return builder(userId, ACCESS, accessExpiration).claim(ROLE_CLAIM, role).compact();
	}

	public String createRefreshToken(Long userId) {
		return builder(userId, REFRESH, refreshExpiration).compact();
	}

	public AccessTokenClaims parseAccessToken(String token) {
		Claims claims = parse(token, ACCESS);
		return new AccessTokenClaims(Long.valueOf(claims.getSubject()), claims.get(ROLE_CLAIM, String.class));
	}

	public Long parseRefreshToken(String token) {
		return Long.valueOf(parse(token, REFRESH).getSubject());
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

	private Claims parse(String token, String expectedType) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
				throw new BusinessException(ErrorCode.INVALID_TOKEN);
			}
			return claims;
		} catch (ExpiredJwtException ex) {
			throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
	}

}
