package com.urijip.server.global.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.urijip.server.global.push.FcmProperties;
import com.urijip.server.global.push.FcmPushSender;
import com.urijip.server.global.push.LoggingPushSender;
import com.urijip.server.global.push.PushSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(FcmProperties.class)
public class PushConfig {

	static final String FIREBASE_APP_NAME = "urijip";

	// 키 파일 경로가 있는데 읽지 못하면 서버를 띄우지 않는다. 경로가 없으면 경고만 남기고 로그 발송기를 쓴다.
	@Bean
	PushSender pushSender(FcmProperties properties) {
		if (!properties.configured()) {
			log.warn("push.fcm.credentials-path is not set. Push notifications will only be logged.");
			return new LoggingPushSender();
		}
		return new FcmPushSender(FirebaseMessaging.getInstance(firebaseApp(properties.credentialsPath())));
	}

	// 테스트처럼 컨텍스트가 여러 번 뜨면 이미 만든 앱을 다시 쓴다.
	private FirebaseApp firebaseApp(String credentialsPath) {
		for (FirebaseApp app : FirebaseApp.getApps()) {
			if (app.getName().equals(FIREBASE_APP_NAME)) {
				return app;
			}
		}
		try (InputStream credentials = new FileInputStream(credentialsPath)) {
			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(credentials))
					.build();
			return FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read Firebase credentials: " + credentialsPath, e);
		}
	}

}
