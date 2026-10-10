package com.urijip.server.global.security;

import java.io.IOException;

import com.urijip.server.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityErrorResponseWriter responseWriter;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		Object recorded = request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE);
		ErrorCode errorCode = recorded instanceof ErrorCode code ? code : ErrorCode.INVALID_TOKEN;
		responseWriter.write(response, errorCode);
	}

}
