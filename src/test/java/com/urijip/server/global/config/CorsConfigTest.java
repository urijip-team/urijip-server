package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.urijip.server.global.security.CorsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigTest {

	@Test
	void allowsConfiguredOriginsWithJwtHeadersAndNoCredentials() {
		CorsConfiguration configuration = configurationFor(new CorsProperties(List.of("https://admin.urijip.com")));

		assertThat(configuration.checkOrigin("https://admin.urijip.com")).isEqualTo("https://admin.urijip.com");
		assertThat(configuration.checkOrigin("https://evil.example.com")).isNull();
		assertThat(configuration.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
		assertThat(configuration.getAllowedMethods()).contains("PATCH", "DELETE");
		assertThat(configuration.getAllowCredentials()).isNull();
	}

	@Test
	void emptyOriginsAllowNothing() {
		CorsConfiguration configuration = configurationFor(new CorsProperties(null));

		assertThat(configuration.checkOrigin("http://localhost:5173")).isNull();
	}

	@Test
	void blankEntriesFromEnvironmentAreDropped() {
		assertThat(new CorsProperties(List.of(" https://a.com ", "")).allowedOrigins()).containsExactly("https://a.com");
	}

	private CorsConfiguration configurationFor(CorsProperties properties) {
		return new CorsConfig().corsConfigurationSource(properties)
				.getCorsConfiguration(new MockHttpServletRequest("GET", "/admin/stats"));
	}

}
