package com.urijip.server.global.config;

import java.net.URI;

import com.urijip.server.global.storage.S3Properties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

	@Bean
	S3Client s3Client(S3Properties properties) {
		var builder = S3Client.builder()
				.region(Region.of(properties.region()))
				.credentialsProvider(credentialsProvider(properties));
		if (hasEndpoint(properties)) {
			// LocalStack은 버킷을 도메인이 아닌 경로로 구분한다.
			builder.endpointOverride(URI.create(properties.endpoint())).forcePathStyle(true);
		}
		return builder.build();
	}

	@Bean
	S3Presigner s3Presigner(S3Properties properties) {
		var builder = S3Presigner.builder()
				.region(Region.of(properties.region()))
				.credentialsProvider(credentialsProvider(properties));
		if (hasEndpoint(properties)) {
			builder.endpointOverride(URI.create(properties.endpoint()))
					.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
		}
		return builder.build();
	}

	private AwsCredentialsProvider credentialsProvider(S3Properties properties) {
		if (properties.accessKey() == null || properties.accessKey().isBlank()) {
			return DefaultCredentialsProvider.builder().build();
		}
		return StaticCredentialsProvider.create(
				AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
	}

	private boolean hasEndpoint(S3Properties properties) {
		return properties.endpoint() != null && !properties.endpoint().isBlank();
	}

}
