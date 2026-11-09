package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// 빈 MySQL에 마이그레이션이 모두 적용되는지 본다. 엔티티와 맞는지는 ddl-auto: validate가 컨텍스트를 띄울 때 검사한다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FlywayMigrationTest {

	@Autowired
	private Flyway flyway;

	@Test
	void allMigrationsAreApplied() {
		assertThat(flyway.info().pending()).isEmpty();
		assertThat(flyway.info().applied()).extracting(MigrationInfo::getVersion)
				.extracting(Object::toString)
				.contains("1");
	}

}
