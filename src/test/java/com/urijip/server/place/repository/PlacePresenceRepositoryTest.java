package com.urijip.server.place.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.place.entity.Place;
import com.urijip.server.place.entity.PlacePresence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class PlacePresenceRepositoryTest {

	@Autowired
	private PlacePresenceRepository placePresenceRepository;

	@Autowired
	private PlaceRepository placeRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User kid;

	private Place home;

	private Place school;

	private Place otherFamilyPlace;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com");
		kid = saveUser("kid@example.com");
		Family kims = familyRepository.saveAndFlush(Family.builder().name("김씨네").inviteCode("A3K9P2").owner(mom).build());
		Family parks = familyRepository.saveAndFlush(Family.builder().name("박씨네").inviteCode("B7Q4M1").owner(mom).build());
		home = placeRepository.save(place(kims, "집"));
		school = placeRepository.save(place(kims, "학교"));
		otherFamilyPlace = placeRepository.saveAndFlush(place(parks, "회사"));
	}

	@Test
	void findPlaceIdsByUserIdReturnsPlacesTheUserIsIn() {
		placePresenceRepository.save(new PlacePresence(home, kid));
		placePresenceRepository.save(new PlacePresence(school, mom));
		placePresenceRepository.saveAndFlush(new PlacePresence(otherFamilyPlace, kid));

		assertThat(placePresenceRepository.findPlaceIdsByUserId(kid.getId()))
				.containsExactlyInAnyOrder(home.getId(), otherFamilyPlace.getId());
	}

	@Test
	void sameUserCannotBeInSamePlaceTwice() {
		placePresenceRepository.saveAndFlush(new PlacePresence(home, kid));

		assertThatThrownBy(() -> placePresenceRepository.saveAndFlush(new PlacePresence(home, kid)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletesByPlaceAndUserByPlaceAndByFamily() {
		placePresenceRepository.save(new PlacePresence(home, kid));
		placePresenceRepository.save(new PlacePresence(home, mom));
		placePresenceRepository.save(new PlacePresence(school, kid));
		placePresenceRepository.saveAndFlush(new PlacePresence(otherFamilyPlace, kid));

		assertThat(placePresenceRepository.deleteByPlaceIdAndUserId(home.getId(), kid.getId())).isEqualTo(1);
		assertThat(placePresenceRepository.deleteByPlaceId(home.getId())).isEqualTo(1);
		assertThat(placePresenceRepository.deleteByFamilyId(school.getFamily().getId())).isEqualTo(1);
		assertThat(placePresenceRepository.findPlaceIdsByUserId(kid.getId())).containsExactly(otherFamilyPlace.getId());
	}

	private Place place(Family family, String name) {
		return new Place(family, mom, name, new BigDecimal("35.1595"), new BigDecimal("126.8526"), 100);
	}

	private User saveUser(String email) {
		return userRepository.saveAndFlush(User.builder().email(email).password("pw").name("회원").build());
	}

}
