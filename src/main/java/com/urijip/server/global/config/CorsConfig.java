package com.urijip.server.global.config;

import java.time.Duration;
import java.util.List;

import com.urijip.server.global.security.CorsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// 인증은 Authorization 헤더의 JWT로 하므로 쿠키(credentials)는 허용하지 않는다.
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setMaxAge(Duration.ofHours(1));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
