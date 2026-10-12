package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiConfigTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void apiDocsAreOpenWithoutToken() {
		assertThat(apiDocs()).hasStatusOk();
		assertThat(apiDocs()).bodyJson().extractingPath("$.info.title").isEqualTo("우리집 API");
	}

	@Test
	void apiDocsDeclareBearerJwtScheme() {
		MvcTestResult result = apiDocs();

		assertThat(result).bodyJson().extractingPath("$.components.securitySchemes.bearerAuth.type").isEqualTo("http");
		assertThat(result).bodyJson().extractingPath("$.components.securitySchemes.bearerAuth.scheme").isEqualTo("bearer");
		assertThat(result).bodyJson().extractingPath("$.components.securitySchemes.bearerAuth.bearerFormat").isEqualTo("JWT");
	}

	@Test
	void apiDocsRequireBearerTokenForAllOperations() {
		assertThat(apiDocs()).bodyJson().extractingPath("$.security[0].bearerAuth").isNotNull();
	}

	@Test
	void authenticatedUserIdIsNotShownAsRequestParameter() {
		MvcTestResult result = apiDocs();

		assertThat(result).bodyJson().extractingPath("$.paths['/api/families'].post.requestBody").isNotNull();
		assertThat(result).bodyText().doesNotContain("\"name\":\"userId\"");
	}

	private MvcTestResult apiDocs() {
		return mvc.get().uri("/v3/api-docs").exchange();
	}

}
