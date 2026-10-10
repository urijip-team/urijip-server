package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

class JwtAccessDeniedHandlerTest {

	private final JsonMapper jsonMapper = new JsonMapper();

	private final JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(
			new SecurityErrorResponseWriter(jsonMapper));

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	@BeforeEach
	void handle() throws Exception {
		handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));
	}

	@Test
	void respondsWithForbidden() {
		assertThat(response.getStatus()).isEqualTo(403);
	}

}
