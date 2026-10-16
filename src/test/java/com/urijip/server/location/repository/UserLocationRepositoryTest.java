package com.urijip.server.location.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.location.entity.UserLocation;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class UserLocationRepositoryTest {

	@Autowired
	private UserLocationRepository userLocationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private User mom;

	@BeforeEach
	void setUp() {
		mom = userRepository.saveAndFlush(User.builder()
				.email("mom@example.com")
				.password("encoded-password")
				.name("김엄마")
				.build());
	}

	@Test
	void newLocationUsesUserIdAsIdAndSharesByDefault() {
		UserLocation saved = userLocationRepository.saveAndFlush(new UserLocation(mom));

		assertThat(saved.getUserId()).isEqualTo(mom.getId());
		assertThat(saved.isSharingEnabled()).isTrue();
		assertThat(saved.getLatitude()).isNull();
		assertThat(saved.getUpdatedAt()).isNull();
	}

	@Test
	void savedLocationKeepsSevenDecimalPlaces() {
		UserLocation location = new UserLocation(mom);
		location.updateLocation(new BigDecimal("37.5665123"), new BigDecimal("-126.9780456"),
				LocalDateTime.of(2026, 10, 17, 14, 0));
		userLocationRepository.saveAndFlush(location);
		entityManager.clear();

		UserLocation found = userLocationRepository.findById(mom.getId()).orElseThrow();

		assertThat(found.getLatitude()).isEqualByComparingTo("37.5665123");
		assertThat(found.getLongitude()).isEqualByComparingTo("-126.9780456");
		assertThat(found.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 14, 0));
	}

	@Test
	void turningSharingOffClearsLocation() {
		UserLocation location = new UserLocation(mom);
		location.updateLocation(new BigDecimal("37.5665"), new BigDecimal("126.978"), LocalDateTime.now());

		location.changeSharing(false);

		assertThat(location.isSharingEnabled()).isFalse();
		assertThat(location.getLatitude()).isNull();
		assertThat(location.getLongitude()).isNull();
		assertThat(location.getUpdatedAt()).isNull();
	}

}
