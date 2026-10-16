package com.urijip.server.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 계좌번호 암호화 키. Base64로 인코딩한 32바이트(AES-256) 값이다.
 */
@ConfigurationProperties("aes")
public record AesProperties(String key) {
}
