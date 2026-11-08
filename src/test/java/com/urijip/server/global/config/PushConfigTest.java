package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

import com.urijip.server.global.push.FcmPushSender;
import com.urijip.server.global.push.LoggingPushSender;
import com.urijip.server.global.push.PushSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PushConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(PushConfig.class);

	@Test
	void usesLoggingSenderWithoutCredentials() {
		runner.run(context -> assertThat(context.getBean(PushSender.class)).isInstanceOf(LoggingPushSender.class));
	}

	@Test
	void usesLoggingSenderWithBlankCredentials() {
		runner.withPropertyValues("push.fcm.credentials-path= ")
				.run(context -> assertThat(context.getBean(PushSender.class)).isInstanceOf(LoggingPushSender.class));
	}

	@Test
	void usesFcmSenderWithServiceAccountKey(@TempDir Path dir) throws Exception {
		Path key = writeServiceAccountKey(dir);

		runner.withPropertyValues("push.fcm.credentials-path=" + key)
				.run(context -> assertThat(context.getBean(PushSender.class)).isInstanceOf(FcmPushSender.class));
	}

	@Test
	void failsToStartWithMissingKeyFile(@TempDir Path dir) {
		runner.withPropertyValues("push.fcm.credentials-path=" + dir.resolve("missing.json"))
				.run(context -> assertThat(context).hasFailed());
	}

	// 네트워크 없이 읽을 수 있는 가짜 서비스 계정 키. 개인 키만 진짜 형식이면 된다.
	private static Path writeServiceAccountKey(Path dir) throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		String privateKey = "-----BEGIN PRIVATE KEY-----\\n"
				+ Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
				+ "\\n-----END PRIVATE KEY-----\\n";
		String json = """
				{
				  "type": "service_account",
				  "project_id": "urijip-test",
				  "private_key_id": "test",
				  "private_key": "%s",
				  "client_email": "push@urijip-test.iam.gserviceaccount.com",
				  "client_id": "1",
				  "token_uri": "https://oauth2.googleapis.com/token"
				}
				""".formatted(privateKey);
		return Files.writeString(dir.resolve("firebase.json"), json);
	}

}
