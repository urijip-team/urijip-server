package com.urijip.server.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.entity.FamilyStatus;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.place.dto.request.PlaceCreateRequest;
import com.urijip.server.place.dto.request.PlaceUpdateRequest;
import com.urijip.server.place.dto.response.PlaceResponse;
import com.urijip.server.place.entity.Place;
import com.urijip.server.place.entity.PlacePresence;
import com.urijip.server.place.repository.PlacePresenceRepository;
import com.urijip.server.place.repository.PlaceRepository;
import jakarta.persistence.EntityManager;
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
	private PlacePresenceRepository placePresenceRepository;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

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

	@Test
	void getPlacesListsFamilyPlacesInOrder() {
		PlaceResponse home = create(mom, "집", null);
		PlaceResponse school = create(kid, "학교", 300);

		assertThat(placeService.getPlaces(familyId, kid.getId())).containsExactly(home, school);
	}

	@Test
	void getPlacesByStrangerFails() {
		assertError(() -> placeService.getPlaces(familyId, stranger.getId()), ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void anyMemberCanUpdateOnlySentFields() {
		PlaceResponse home = create(mom, "집", null);

		PlaceResponse updated = placeService.update(home.id(), kid.getId(),
				new PlaceUpdateRequest(null, new BigDecimal("35.2"), new BigDecimal("126.9"), 150));

		assertThat(updated.name()).isEqualTo("집");
		assertThat(updated.latitude()).isEqualTo(35.2);
		assertThat(updated.longitude()).isEqualTo(126.9);
		assertThat(updated.radiusMeters()).isEqualTo(150);
	}

	@Test
	void updateByStrangerOrOfUnknownPlaceFails() {
		PlaceResponse home = create(mom, "집", null);
		PlaceUpdateRequest rename = new PlaceUpdateRequest("새 집", null, null, null);

		assertError(() -> placeService.update(home.id(), stranger.getId(), rename), ErrorCode.FAMILY_NOT_MEMBER);
		assertError(() -> placeService.update(-1L, mom.getId(), rename), ErrorCode.PLACE_NOT_FOUND);
	}

	@Test
	void anyMemberCanDelete() {
		PlaceResponse home = create(mom, "집", null);
		placePresenceRepository.save(new PlacePresence(placeRepository.getReferenceById(home.id()), kid));
		// 기록은 벌크 삭제되므로 트랜잭션 경계를 흉내 낸다.
		entityManager.flush();
		entityManager.clear();

		placeService.delete(home.id(), kid.getId());
		entityManager.flush();

		assertThat(placeRepository.findById(home.id())).isEmpty();
	}

	@Test
	void deleteByStrangerFails() {
		PlaceResponse home = create(mom, "집", null);

		assertError(() -> placeService.delete(home.id(), stranger.getId()), ErrorCode.FAMILY_NOT_MEMBER);
		assertThat(placeRepository.findById(home.id())).isPresent();
	}

	@Test
	void deletingFamilyDeletesItsPlaces() {
		PlaceResponse home = create(mom, "집", null);
		placePresenceRepository.save(new PlacePresence(placeRepository.getReferenceById(home.id()), kid));
		entityManager.flush();
		entityManager.clear();

		familyService.leaveFamily(kid.getId());
		familyService.leaveFamily(mom.getId());

		assertThat(placeRepository.count()).isZero();
		assertThat(placePresenceRepository.count()).isZero();
	}

	private PlaceResponse create(User user, String name, Integer radius) {
		return placeService.create(familyId, user.getId(), new PlaceCreateRequest(name, new BigDecimal("35.1595"),
				new BigDecimal("126.8526"), radius));
	}

	private void assertError(Runnable action, ErrorCode errorCode) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(errorCode));
	}

	@Test
	void placesOfSuspendedFamilyAreBlocked() {
		familyService.getFamily(familyId).changeStatus(FamilyStatus.SUSPENDED);

		assertThatThrownBy(() -> placeService.getPlaces(familyId, mom.getId()))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FAMILY_SUSPENDED));
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder().email(email).password("encoded-password").name(name).build());
	}

}
