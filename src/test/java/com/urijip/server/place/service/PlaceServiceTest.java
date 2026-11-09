package com.urijip.server.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.place.dto.request.PlaceCreateRequest;
import com.urijip.server.place.dto.response.PlaceResponse;
import com.urijip.server.place.entity.Place;
import com.urijip.server.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PlaceServiceTest {

	@Autowired
	private PlaceService placeService;

	@Autowired
	private PlaceRepository placeRepository;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User kid;

	private User stranger;

	private Long familyId;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		kid = saveUser("kid@example.com", "김첫째");
		stranger = saveUser("stranger@example.com", "남");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("우리집"));
		familyService.join(kid.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyId = family.id();
	}

	@Test
	void createSavesPlaceWithDefaultRadius() {
		PlaceResponse response = create(kid, "학교", null);

		assertThat(response.name()).isEqualTo("학교");
		assertThat(response.latitude()).isEqualTo(35.1595);
		assertThat(response.longitude()).isEqualTo(126.8526);
		assertThat(response.radiusMeters()).isEqualTo(100);
		assertThat(response.createdAt().getOffset().getId()).isEqualTo("+09:00");
		Place saved = placeRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getFamily().getId()).isEqualTo(familyId);
		assertThat(saved.getCreatedBy().getId()).isEqualTo(kid.getId());
	}

	@Test
	void createRoundsCoordinatesToSevenDecimals() {
		PlaceResponse response = placeService.create(familyId, mom.getId(), new PlaceCreateRequest("집",
				new BigDecimal("35.12345678"), new BigDecimal("126.12345674"), 200));

		assertThat(response.latitude()).isEqualTo(35.1234568);
		assertThat(response.longitude()).isEqualTo(126.1234567);
		assertThat(response.radiusMeters()).isEqualTo(200);
	}

	@Test
	void createByStrangerFails() {
		assertError(() -> create(stranger, "집", null), ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void eleventhPlaceIsRejected() {
		for (int i = 0; i < Place.MAX_PLACES_PER_FAMILY; i++) {
			create(mom, "장소 " + i, null);
		}

		assertError(() -> create(mom, "하나 더", null), ErrorCode.PLACE_LIMIT_EXCEEDED);
	}

	private PlaceResponse create(User user, String name, Integer radius) {
		return placeService.create(familyId, user.getId(), new PlaceCreateRequest(name, new BigDecimal("35.1595"),
				new BigDecimal("126.8526"), radius));
	}

	private void assertError(Runnable action, ErrorCode errorCode) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(errorCode));
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder().email(email).password("encoded-password").name(name).build());
	}

}
