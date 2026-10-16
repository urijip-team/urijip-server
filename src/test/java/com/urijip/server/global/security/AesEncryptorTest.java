package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class AesEncryptorTest {

	private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

	private final AesEncryptor encryptor = new AesEncryptor(new AesProperties(KEY));

	@Test
	void decryptReturnsOriginalText() {
		String encrypted = encryptor.encrypt("12345678901234");

		assertThat(encryptor.decrypt(encrypted)).isEqualTo("12345678901234");
	}

	@Test
	void encryptedTextDoesNotContainPlainText() {
		String encrypted = encryptor.encrypt("12345678901234");

		assertThat(encrypted).doesNotContain("12345678901234");
	}

	@Test
	void sameTextIsEncryptedDifferentlyEachTime() {
		assertThat(encryptor.encrypt("12345678901234")).isNotEqualTo(encryptor.encrypt("12345678901234"));
	}

	@Test
	void decryptHandlesKorean() {
		assertThat(encryptor.decrypt(encryptor.encrypt("김엄마"))).isEqualTo("김엄마");
	}

	@Test
	void decryptWithAnotherKeyFails() {
		String encrypted = encryptor.encrypt("12345678901234");
		byte[] otherKey = new byte[32];
		otherKey[0] = 1;
		AesEncryptor other = new AesEncryptor(new AesProperties(Base64.getEncoder().encodeToString(otherKey)));

		assertThatThrownBy(() -> other.decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void decryptRejectsTamperedText() {
		byte[] encrypted = Base64.getDecoder().decode(encryptor.encrypt("12345678901234"));
		encrypted[encrypted.length - 1] ^= 1;

		assertThatThrownBy(() -> encryptor.decrypt(Base64.getEncoder().encodeToString(encrypted)))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void keyMustBe32Bytes() {
		String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

		assertThatThrownBy(() -> new AesEncryptor(new AesProperties(shortKey)))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
