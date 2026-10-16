package com.urijip.server.global.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 사진을 저장하는 S3 버킷 설정.
 * endpoint는 LocalStack처럼 S3 대신 쓰는 서버의 주소다. 운영에서는 비워 두고 실제 S3를 쓴다.
 * accessKey가 없으면 AWS 기본 자격 증명(환경변수, IAM 역할 등)을 쓴다.
 * 버킷은 비공개이고, 파일은 urlExpiration 동안만 유효한 서명 URL로 내보낸다.
 */
@ConfigurationProperties("storage.s3")
public record S3Properties(String bucket, String region, String endpoint, String accessKey, String secretKey,
		@DefaultValue("1h") Duration urlExpiration) {
}
