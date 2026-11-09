package com.urijip.server.place.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.request.NicknameUpdateRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.push.PushType;
import com.urijip.server.location.dto.request.LocationSharingRequest;
import com.urijip.server.location.dto.request.LocationUpdateRequest;
import com.urijip.server.location.service.LocationService;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notification.service.PushRequestedEvent;
import com.urijip.server.place.dto.request.PlaceCreateRequest;
import com.urijip.server.place.repository.PlacePresenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

// 집은 (35.1595, 126.8526), 반경 100m. 위도 0.001도는 약 111m다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RecordApplicationEvents
class PlaceAlertServiceTest {

	private static final String HOME_LAT = "35.1595";

	private static final String LNG = "126.8526";

	@Autowired
	private LocationService locationService;

	@Autowired
	private PlaceService placeService;

	@Autowired
	private PlacePresenceRepository placePresenceRepository;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User kid;

	private Long homeId;

	private Long familyId;

	@Autowired
	private ApplicationEvents events;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		kid = saveUser("kid@example.com", "김첫째");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("우리집"));
		familyService.join(kid.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyId = family.id();
		homeId = placeService.create(family.id(), mom.getId(), new PlaceCreateRequest("집",
				new BigDecimal(HOME_LAT), new BigDecimal(LNG), 100)).id();
	}

	@Test
	void enteringRadiusRecordsPresence() {
		moveKid("35.1620");
		assertThat(insidePlaces()).isEmpty();

		moveKid("35.1600");

		assertThat(insidePlaces()).containsExactly(homeId);
	}

	@Test
	void stayingInsideKeepsOneRecord() {
		moveKid("35.1600");
		moveKid("35.1595");

		assertThat(placePresenceRepository.count()).isEqualTo(1);
	}

	@Test
	void leavingNeedsMarginBeyondRadius() {
		moveKid("35.1595");

		// 약 117m: 반경(100m)은 넘었지만 여유(30m) 안이라 아직 안에 있는 것으로 본다.
		moveKid("35.16055");
		assertThat(insidePlaces()).containsExactly(homeId);

		// 약 144m: 반경 + 30m를 넘었다.
		moveKid("35.1608");
		assertThat(insidePlaces()).isEmpty();
	}

	@Test
	void locationIsIgnoredWhileSharingIsOff() {
		locationService.changeSharing(kid.getId(), new LocationSharingRequest(false));

		moveKid("35.1595");

		assertThat(insidePlaces()).isEmpty();
	}

	@Test
	void arrivalAndLeavingArePushedToFamilyExceptMover() {
		familyService.changeMyNickname(familyId, kid.getId(), new NicknameUpdateRequest("첫째"));
		moveKid("35.1620");

		moveKid("35.1595");
		moveKid("35.1620");

		assertThat(events.stream(PushRequestedEvent.class)).satisfiesExactly(
				arrived -> {
					assertThat(arrived.userIds()).isEqualTo(Set.of(mom.getId()));
					assertThat(arrived.message().type()).isEqualTo(PushType.PLACE);
					assertThat(arrived.message().title()).isEqualTo("집");
					assertThat(arrived.message().body()).isEqualTo("첫째님이 집에 도착했어요.");
					assertThat(arrived.message().data()).isEqualTo(Map.of("placeId", homeId.toString(),
							"userId", kid.getId().toString()));
				},
				left -> assertThat(left.message().body()).isEqualTo("첫째님이 집에서 출발했어요."));
	}

	@Test
	void movingAroundOutsideOrInsidePushesNothingMore() {
		moveKid("35.1620");
		moveKid("35.1630");
		moveKid("35.1595");
		moveKid("35.1596");

		assertThat(events.stream(PushRequestedEvent.class)).hasSize(1);
	}

	private void moveKid(String latitude) {
		locationService.updateMyLocation(kid.getId(), new LocationUpdateRequest(new BigDecimal(latitude),
				new BigDecimal(LNG)));
	}

	private List<Long> insidePlaces() {
		return placePresenceRepository.findPlaceIdsByUserId(kid.getId());
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder().email(email).password("encoded-password").name(name).build());
	}

}
