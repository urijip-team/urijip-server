package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import com.urijip.server.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AesPropertiesTest {

	@Autowired
	private AesProperties aesProperties;

	@Test
	void localKeyIs32Bytes() {
		assertThat(Base64.getDecoder().decode(aesProperties.key())).hasSize(32);
	}

}
