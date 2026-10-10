package com.urijip.server.global.security;

import java.io.IOException;
import java.util.List;

import com.urijip.server.global.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String ERROR_CODE_ATTRIBUTE = "jwtErrorCode";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String token = resolveToken(request);
		if (token != null) {
			authenticate(token, request);
		}
		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return header.substring(BEARER_PREFIX.length());
	}

	private void authenticate(String token, HttpServletRequest request) {
		try {
			AccessTokenClaims claims = jwtProvider.parseAccessToken(token);
			Authentication authentication = new UsernamePasswordAuthenticationToken(claims.userId(), null,
					List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (BusinessException ex) {
			request.setAttribute(ERROR_CODE_ATTRIBUTE, ex.getErrorCode());
		}
	}

}
