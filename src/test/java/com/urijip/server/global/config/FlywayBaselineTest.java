package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

// 이미 테이블이 있는 DB(Flyway 전에 Hibernate가 만든 DB)에서 V1을 건너뛰는지 본다. 설정은 application.yaml과 같게 준다.
class FlywayBaselineTest {

	private static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	private DataSource dataSource;

	@BeforeEach
	void setUp() {
		MYSQL.start();
		dataSource = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
	}

	@AfterEach
	void tearDown() {
		MYSQL.stop();
	}

	@Test
	void existingSchemaIsBaselinedAtVersionOne() {
		new JdbcTemplate(dataSource).execute("CREATE TABLE users (id bigint PRIMARY KEY)");

		flyway().migrate();

		assertThat(flyway().info().applied()).extracting(MigrationInfo::getVersion)
				.extracting(Object::toString)
				.containsExactly("1");
		assertThat(flyway().info().applied()[0].getType().isBaseline()).isTrue();
	}

	@Test
	void emptySchemaGetsVersionOneApplied() {
		flyway().migrate();

		assertThat(flyway().info().applied()[0].getType().isBaseline()).isFalse();
		assertThat(new JdbcTemplate(dataSource).queryForObject(
				"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'places'",
				Integer.class)).isEqualTo(1);
	}

	private Flyway flyway() {
		return Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).baselineVersion("1").load();
	}

}
