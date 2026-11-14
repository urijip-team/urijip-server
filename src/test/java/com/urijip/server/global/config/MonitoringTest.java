package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

// 프로메테우스가 가져가는 지표와 로그의 traceId를 확인한다(로컬 프로파일).
// 테스트는 기본으로 지표 내보내기를 끄므로 @AutoConfigureMetrics로 켠다.
@SpringBootTest
@AutoConfigureMetrics
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MonitoringTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void prometheusEndpointIsOpenWithoutToken() {
		MvcTestResult result = mvc.get().uri("/actuator/prometheus").exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyText().contains("jvm_memory_used_bytes");
	}

	@Test
	void metricsAreTaggedWithApplicationName() {
		assertThat(mvc.get().uri("/actuator/prometheus").exchange())
				.bodyText().contains("application=\"urijip-server\"");
	}

	// p95 응답 시간을 계산하려면 요청 지표가 히스토그램 버킷으로 나와야 한다.
	@Test
	void httpRequestsAreExportedAsHistogram() {
		mvc.get().uri("/actuator/health").exchange();

		assertThat(mvc.get().uri("/actuator/prometheus").exchange())
				.bodyText().contains("http_server_requests_seconds_bucket");
	}

}
