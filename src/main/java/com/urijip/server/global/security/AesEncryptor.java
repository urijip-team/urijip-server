package com.urijip.server.global.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * AES-256-GCM으로 문자열을 암호화한다. 결과는 Base64(IV 12바이트 + 암호문)이고, 같은 값도 매번 다르게 암호화된다.
 */
@Component
public class AesEncryptor {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int KEY_LENGTH = 32;
	private static final int IV_LENGTH = 12;
	private static final int TAG_BITS = 128;

	private final SecretKey key;
	private final SecureRandom random = new SecureRandom();

	public AesEncryptor(AesProperties properties) {
		byte[] keyBytes = Base64.getDecoder().decode(properties.key());
		if (keyBytes.length != KEY_LENGTH) {
			throw new IllegalArgumentException("aes.key는 Base64로 인코딩한 32바이트여야 합니다");
		}
		this.key = new SecretKeySpec(keyBytes, "AES");
	}

	public String encrypt(String plainText) {
		byte[] iv = new byte[IV_LENGTH];
		random.nextBytes(iv);
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(IV_LENGTH + encrypted.length)
					.put(iv)
					.put(encrypted)
					.array());
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("암호화에 실패했습니다", ex);
		}
	}

	public String decrypt(String cipherText) {
		byte[] decoded = Base64.getDecoder().decode(cipherText);
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, decoded, 0, IV_LENGTH));
			byte[] decrypted = cipher.doFinal(decoded, IV_LENGTH, decoded.length - IV_LENGTH);
			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("복호화에 실패했습니다", ex);
		}
	}

}
