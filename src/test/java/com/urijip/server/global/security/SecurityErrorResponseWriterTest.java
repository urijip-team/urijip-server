package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class SecurityErrorResponseWriterTest {

	private final JsonMapper jsonMapper = new JsonMapper();

	private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(jsonMapper);

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	@Test
	void writesStatusOfErrorCode() throws Exception {
		writer.write(response, ErrorCode.TOKEN_EXPIRED);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
	}

}
