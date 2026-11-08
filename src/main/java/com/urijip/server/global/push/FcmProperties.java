package com.urijip.server.global.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FCM 설정. credentialsPath는 Firebase 서비스 계정 키(JSON) 파일의 경로다.
 * 비어 있으면 푸시를 보내지 않고 로그만 남긴다(로컬, 테스트). 운영은 FIREBASE_CREDENTIALS_PATH로 준다.
 */
@ConfigurationProperties("push.fcm")
public record FcmProperties(String credentialsPath) {

	public boolean configured() {
		return credentialsPath != null && !credentialsPath.isBlank();
	}

}
