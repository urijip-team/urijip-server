package com.urijip.server.sos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.sos.entity.SosAlert;
import com.urijip.server.sos.entity.SosStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class SosAlertRepositoryTest {

	private static final BigDecimal LATITUDE = new BigDecimal("35.1595000");

	private static final BigDecimal LONGITUDE = new BigDecimal("126.8526000");

	@Autowired
	private SosAlertRepository sosAlertRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private Family kims;

	private Family parks;

	@BeforeEach
	void setUp() {
		mom = userRepository.saveAndFlush(User.builder()
				.email("mom@example.com")
				.password("encoded-password")
				.name("김엄마")
				.build());
		kims = familyRepository.saveAndFlush(Family.builder().name("김씨네").inviteCode("A3K9P2").owner(mom).build());
		parks = familyRepository.saveAndFlush(Family.builder().name("박씨네").inviteCode("B7Q4M1").owner(mom).build());
	}

	@Test
	void newAlertIsActiveWithoutResolver() {
		SosAlert alert = sosAlertRepository.saveAndFlush(new SosAlert(kims, mom, LATITUDE, LONGITUDE));

		SosAlert found = sosAlertRepository.findById(alert.getId()).orElseThrow();
		assertThat(found.getFamily().getId()).isEqualTo(kims.getId());
		assertThat(found.getUser().getId()).isEqualTo(mom.getId());
		assertThat(found.getLatitude()).isEqualByComparingTo(LATITUDE);
		assertThat(found.getLongitude()).isEqualByComparingTo(LONGITUDE);
		assertThat(found.getStatus()).isEqualTo(SosStatus.ACTIVE);
		assertThat(found.getResolvedBy()).isNull();
		assertThat(found.getResolvedAt()).isNull();
		assertThat(found.getCreatedAt()).isNotNull();
	}

	@Test
	void resolveRecordsResolverAndTime() {
		SosAlert alert = save(kims);

		alert.resolve(mom);
		sosAlertRepository.saveAndFlush(alert);

		SosAlert found = sosAlertRepository.findById(alert.getId()).orElseThrow();
		assertThat(found.isResolved()).isTrue();
		assertThat(found.getStatus()).isEqualTo(SosStatus.RESOLVED);
		assertThat(found.getResolvedBy().getId()).isEqualTo(mom.getId());
		assertThat(found.getResolvedAt()).isNotNull();
	}

	private SosAlert save(Family family) {
		return sosAlertRepository.saveAndFlush(new SosAlert(family, mom, LATITUDE, LONGITUDE));
	}

}
