package com.urijip.server.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.place.entity.Place;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class PlaceRepositoryTest {

	@Autowired
	private PlaceRepository placeRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private User mom;

	private Family kims;

	private Family parks;

	@BeforeEach
	void setUp() {
		mom = userRepository.saveAndFlush(User.builder().email("mom@example.com").password("pw").name("김엄마").build());
		kims = familyRepository.saveAndFlush(Family.builder().name("김씨네").inviteCode("A3K9P2").owner(mom).build());
		parks = familyRepository.saveAndFlush(Family.builder().name("박씨네").inviteCode("B7Q4M1").owner(mom).build());
	}

	@Test
	void savedPlaceKeepsLocationRadiusAndCreatedAt() {
		Place saved = placeRepository.saveAndFlush(place(kims, "집"));
		entityManager.clear();

		Place found = placeRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getName()).isEqualTo("집");
		assertThat(found.getLatitude()).isEqualByComparingTo("35.1595000");
		assertThat(found.getLongitude()).isEqualByComparingTo("126.8526000");
		assertThat(found.getRadiusMeters()).isEqualTo(100);
		assertThat(found.getCreatedBy().getId()).isEqualTo(mom.getId());
		assertThat(found.getCreatedAt()).isNotNull();
	}

	@Test
	void findsAndCountsPlacesOfFamily() {
		Place home = placeRepository.save(place(kims, "집"));
		Place school = placeRepository.save(place(kims, "학교"));
		placeRepository.saveAndFlush(place(parks, "회사"));

		assertThat(placeRepository.findByFamilyIdOrderByIdAsc(kims.getId())).extracting(Place::getId)
				.containsExactly(home.getId(), school.getId());
		assertThat(placeRepository.countByFamilyId(kims.getId())).isEqualTo(2);
	}

	private Place place(Family family, String name) {
		return new Place(family, mom, name, new BigDecimal("35.1595000"), new BigDecimal("126.8526000"), 100);
	}

}
