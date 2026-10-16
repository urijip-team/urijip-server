package com.urijip.server.location.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.location.dto.request.LocationSharingRequest;
import com.urijip.server.location.dto.request.LocationUpdateRequest;
import com.urijip.server.location.dto.response.FamilyLocationResponse;
import com.urijip.server.location.dto.response.LocationSharingResponse;
import com.urijip.server.location.entity.UserLocation;
import com.urijip.server.location.repository.UserLocationRepository;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class LocationServiceTest {

	private static final LocationUpdateRequest SEOUL = new LocationUpdateRequest(new BigDecimal("37.5665"),
			new BigDecimal("126.978"));

	@Autowired
	private LocationService locationService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserLocationRepository userLocationRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User dad;

	private User stranger;

	private Long familyId;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		dad = saveUser("dad@example.com", "김아빠");
		stranger = saveUser("stranger@example.com", "남");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("우리집"));
		familyService.join(dad.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyId = family.id();
	}

	@Test
	void updateMyLocationCreatesLocation() {
		locationService.updateMyLocation(mom.getId(), SEOUL);

		UserLocation saved = userLocationRepository.findById(mom.getId()).orElseThrow();
		assertThat(saved.getLatitude()).isEqualByComparingTo("37.5665");
		assertThat(saved.getLongitude()).isEqualByComparingTo("126.978");
		assertThat(saved.getUpdatedAt()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
	}

	@Test
	void updateMyLocationOverwritesLastLocation() {
		locationService.updateMyLocation(mom.getId(), SEOUL);

		locationService.updateMyLocation(mom.getId(),
				new LocationUpdateRequest(new BigDecimal("35.1595"), new BigDecimal("126.8526")));

		assertThat(userLocationRepository.count()).isEqualTo(1);
		assertThat(userLocationRepository.findById(mom.getId()).orElseThrow().getLatitude())
				.isEqualByComparingTo("35.1595");
	}

	@Test
	void updateMyLocationRoundsToSevenDecimalPlaces() {
		locationService.updateMyLocation(mom.getId(),
				new LocationUpdateRequest(new BigDecimal("37.123456789"), new BigDecimal("126.000000049")));

		UserLocation saved = userLocationRepository.findById(mom.getId()).orElseThrow();
		assertThat(saved.getLatitude()).isEqualTo(new BigDecimal("37.1234568"));
		assertThat(saved.getLongitude()).isEqualTo(new BigDecimal("126.0000000"));
	}

	@Test
	void updateMyLocationIsIgnoredWhileSharingIsOff() {
		locationService.changeSharing(mom.getId(), new LocationSharingRequest(false));

		locationService.updateMyLocation(mom.getId(), SEOUL);

		UserLocation saved = userLocationRepository.findById(mom.getId()).orElseThrow();
		assertThat(saved.getLatitude()).isNull();
		assertThat(saved.getUpdatedAt()).isNull();
	}

	@Test
	void updateMyLocationOfUnknownUserThrowsUserNotFound() {
		assertThatThrownBy(() -> locationService.updateMyLocation(999_999L, SEOUL))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void changeSharingOffClearsLocation() {
		locationService.updateMyLocation(mom.getId(), SEOUL);

		LocationSharingResponse response = locationService.changeSharing(mom.getId(),
				new LocationSharingRequest(false));

		assertThat(response.sharingEnabled()).isFalse();
		UserLocation saved = userLocationRepository.findById(mom.getId()).orElseThrow();
		assertThat(saved.isSharingEnabled()).isFalse();
		assertThat(saved.getLatitude()).isNull();
	}

	@Test
	void changeSharingOnAgainAcceptsNewLocation() {
		locationService.changeSharing(mom.getId(), new LocationSharingRequest(false));

		LocationSharingResponse response = locationService.changeSharing(mom.getId(),
				new LocationSharingRequest(true));
		locationService.updateMyLocation(mom.getId(), SEOUL);

		assertThat(response.sharingEnabled()).isTrue();
		assertThat(userLocationRepository.findById(mom.getId()).orElseThrow().getLatitude())
				.isEqualByComparingTo("37.5665");
	}

	@Test
	void changeSharingWithoutLocationCreatesRow() {
		LocationSharingResponse response = locationService.changeSharing(mom.getId(),
				new LocationSharingRequest(true));

		assertThat(response.sharingEnabled()).isTrue();
		assertThat(userLocationRepository.findById(mom.getId())).isPresent();
	}

	@Test
	void getFamilyLocationsReturnsAllMembersInJoinOrder() {
		locationService.updateMyLocation(mom.getId(), SEOUL);

		List<FamilyLocationResponse> locations = locationService.getFamilyLocations(familyId, dad.getId());

		assertThat(locations).extracting(FamilyLocationResponse::userId).containsExactly(mom.getId(), dad.getId());
		FamilyLocationResponse momLocation = locations.get(0);
		assertThat(momLocation.name()).isEqualTo("김엄마");
		assertThat(momLocation.sharingEnabled()).isTrue();
		assertThat(momLocation.latitude()).isEqualTo(37.5665);
		assertThat(momLocation.longitude()).isEqualTo(126.978);
		assertThat(momLocation.updatedAt().getOffset().getTotalSeconds()).isEqualTo(9 * 60 * 60);
		assertThat(momLocation.updatedAt()).isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));
	}

	@Test
	void getFamilyLocationsShowsNullLocationForMemberWhoNeverSent() {
		List<FamilyLocationResponse> locations = locationService.getFamilyLocations(familyId, mom.getId());

		FamilyLocationResponse dadLocation = locations.get(1);
		assertThat(dadLocation.sharingEnabled()).isTrue();
		assertThat(dadLocation.latitude()).isNull();
		assertThat(dadLocation.longitude()).isNull();
		assertThat(dadLocation.updatedAt()).isNull();
	}

	@Test
	void getFamilyLocationsHidesLocationOfMemberWhoTurnedSharingOff() {
		locationService.updateMyLocation(dad.getId(), SEOUL);
		locationService.changeSharing(dad.getId(), new LocationSharingRequest(false));

		FamilyLocationResponse dadLocation = locationService.getFamilyLocations(familyId, mom.getId()).get(1);

		assertThat(dadLocation.sharingEnabled()).isFalse();
		assertThat(dadLocation.latitude()).isNull();
		assertThat(dadLocation.updatedAt()).isNull();
	}

	@Test
	void getFamilyLocationsOfOtherFamilyThrowsFamilyNotMember() {
		assertThatThrownBy(() -> locationService.getFamilyLocations(familyId, stranger.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getFamilyLocationsExcludesUserWhoLeftFamily() {
		locationService.updateMyLocation(dad.getId(), SEOUL);
		familyService.removeMember(familyId, dad.getId(), dad.getId());

		List<FamilyLocationResponse> locations = locationService.getFamilyLocations(familyId, mom.getId());

		assertThat(locations).extracting(FamilyLocationResponse::userId).containsExactly(mom.getId());
	}

	@Test
	void deleteLocationRemovesRow() {
		locationService.updateMyLocation(mom.getId(), SEOUL);

		locationService.deleteLocation(mom.getId());

		assertThat(userLocationRepository.findById(mom.getId())).isEmpty();
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
