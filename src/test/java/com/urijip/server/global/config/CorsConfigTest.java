package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.urijip.server.global.security.CorsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(CorsConfig.class);

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

	// 운영은 환경변수 하나(CORS_ALLOWED_ORIGINS)에 쉼표로 여러 출처를 준다. 비어 있으면 아무것도 허용하지 않는다.
	@Test
	void bindsCommaSeparatedOriginsFromOneProperty() {
		runner.withPropertyValues("cors.allowed-origins=https://admin.urijip.com, https://staging-admin.urijip.com")
				.run(context -> assertThat(context.getBean(CorsProperties.class).allowedOrigins())
						.containsExactly("https://admin.urijip.com", "https://staging-admin.urijip.com"));
		runner.withPropertyValues("cors.allowed-origins=")
				.run(context -> assertThat(context.getBean(CorsProperties.class).allowedOrigins()).isEmpty());
	}

	private CorsConfiguration configurationFor(CorsProperties properties) {
		return new CorsConfig().corsConfigurationSource(properties)
				.getCorsConfiguration(new MockHttpServletRequest("GET", "/admin/stats"));
	}

}
