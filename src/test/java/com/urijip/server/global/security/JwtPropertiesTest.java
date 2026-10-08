package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.urijip.server.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JwtPropertiesTest {

	@Autowired
	private JwtProperties jwtProperties;

	@Test
	void expirationsFollowTheSpec() {
		assertThat(jwtProperties.accessExpiration()).isEqualTo(Duration.ofMinutes(30));
		assertThat(jwtProperties.refreshExpiration()).isEqualTo(Duration.ofDays(14));
	}

}
