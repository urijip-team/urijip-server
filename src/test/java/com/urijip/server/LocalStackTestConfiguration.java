package com.urijip.server;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

// 사진 테스트용 S3. compose.yaml과 같은 이미지·버킷 생성 스크립트를 쓴다.
// S3를 실제로 쓰는 테스트에서만 TestcontainersConfiguration과 함께 @Import한다.
@TestConfiguration(proxyBeanMethods = false)
public class LocalStackTestConfiguration {

	@Bean
	LocalStackContainer localStackContainer() {
		return new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.14.0"))
				.withServices("s3")
				.withCopyFileToContainer(MountableFile.forHostPath("docker/localstack/init-s3.sh", 0755),
						"/etc/localstack/init/ready.d/init-s3.sh");
	}

	@Bean
	DynamicPropertyRegistrar localStackProperties(LocalStackContainer localStack) {
		return registry -> {
			registry.add("storage.s3.endpoint", () -> localStack.getEndpoint().toString());
			registry.add("storage.s3.region", localStack::getRegion);
			registry.add("storage.s3.access-key", localStack::getAccessKey);
			registry.add("storage.s3.secret-key", localStack::getSecretKey);
		};
	}

}
