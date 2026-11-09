package com.urijip.server.global.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 브라우저에서 API를 부르는 곳(관리자 웹)의 출처. 예: https://admin.urijip.com
 * 앱은 브라우저가 아니라 CORS와 상관없다. 비어 있으면 어떤 출처도 허용하지 않는다.
 */
@ConfigurationProperties("cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of()
				: allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList();
	}

}
