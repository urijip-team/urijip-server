package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

// 운영(application-prod.yaml)처럼 지표를 다른 포트로 나눴을 때를 확인한다.
// 로드 밸런서가 닿는 서버 포트에는 /readyz·/livez만 있고, 지표는 관리 포트에서만 나온다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"management.server.port=0",
		"management.endpoint.health.probes.add-additional-paths=true"
})
@AutoConfigureMetrics
@Import(TestcontainersConfiguration.class)
class ManagementPortTest {

	private final RestClient client = RestClient.builder()
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
			})
			.build();

	@LocalServerPort
	private int serverPort;

	@LocalManagementPort
	private int managementPort;

	@Test
	void readinessIsOpenOnServerPort() {
		ResponseEntity<String> response = get(serverPort, "/readyz");

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).contains("UP");
	}

	@Test
	void livenessIsOpenOnServerPort() {
		assertThat(get(serverPort, "/livez").getStatusCode().value()).isEqualTo(200);
	}

	@Test
	void metricsAreNotOnServerPort() {
		assertThat(get(serverPort, "/actuator/prometheus").getStatusCode().value()).isEqualTo(404);
	}

	@Test
	void metricsAreOpenOnManagementPortWithoutToken() {
		ResponseEntity<String> response = get(managementPort, "/actuator/prometheus");

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).contains("jvm_memory_used_bytes");
	}

	@Test
	void healthIsOnManagementPort() {
		assertThat(get(managementPort, "/actuator/health").getStatusCode().value()).isEqualTo(200);
	}

	private ResponseEntity<String> get(int port, String path) {
		return client.get().uri("http://localhost:" + port + path).retrieve().toEntity(String.class);
	}

}
