package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

	private static final String SECRET = "test-secret-key-that-is-long-enough-0123456789";

	private final JwtProvider jwtProvider = provider(Duration.ofMinutes(30));

	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider);

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockFilterChain filterChain = new MockFilterChain();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void validTokenRegistersUserIdAndRole() throws Exception {
		doFilter("Bearer " + jwtProvider.createAccessToken(1L, "USER"));

		assertThat(authentication().getPrincipal()).isEqualTo(1L);
		assertThat(authentication().getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_USER");
		assertThat(filterChain.getRequest()).isSameAs(request);
	}

	private void doFilter(String authorization) throws Exception {
		if (authorization != null) {
			request.addHeader("Authorization", authorization);
		}
		filter.doFilter(request, new MockHttpServletResponse(), filterChain);
	}

	private static Authentication authentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	private static JwtProvider provider(Duration accessExpiration) {
		return new JwtProvider(new JwtProperties(SECRET, accessExpiration, Duration.ofDays(14)));
	}

}
