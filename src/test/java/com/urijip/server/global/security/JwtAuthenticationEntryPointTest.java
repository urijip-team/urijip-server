package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.json.JsonMapper;

class JwtAuthenticationEntryPointTest {

	private final JsonMapper jsonMapper = new JsonMapper();

	private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(
			new SecurityErrorResponseWriter(jsonMapper));

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	@Test
	void requestWithoutTokenGetsInvalidToken() throws Exception {
		assertThat(errorCode()).isEqualTo("INVALID_TOKEN");
		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	void expiredTokenGetsTokenExpired() throws Exception {
		request.setAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);

		assertThat(errorCode()).isEqualTo("TOKEN_EXPIRED");
		assertThat(response.getStatus()).isEqualTo(401);
	}

	private String errorCode() throws Exception {
		entryPoint.commence(request, response, new InsufficientAuthenticationException("unauthenticated"));
		return jsonMapper.readTree(response.getContentAsString()).get("error").get("code").asString();
	}

}
