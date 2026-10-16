package com.urijip.server.location.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.location.dto.request.LocationSharingRequest;
import com.urijip.server.location.dto.request.LocationUpdateRequest;
import com.urijip.server.location.dto.response.FamilyLocationResponse;
import com.urijip.server.location.dto.response.LocationSharingResponse;
import com.urijip.server.location.entity.UserLocation;
import com.urijip.server.location.repository.UserLocationRepository;
import com.urijip.server.member.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

	// DB 컬럼 decimal(10, 7)에 맞춘다. 소수 7자리는 약 1cm다.
	private static final int SCALE = 7;

	private final UserLocationRepository userLocationRepository;
	private final UserService userService;
	private final FamilyService familyService;

	@Transactional(readOnly = true)
	public List<FamilyLocationResponse> getFamilyLocations(Long familyId, Long userId) {
		List<FamilyMemberResponse> members = familyService.getMembers(familyId, userId);
		Map<Long, UserLocation> locations = userLocationRepository
				.findAllById(members.stream().map(FamilyMemberResponse::userId).toList()).stream()
				.collect(Collectors.toMap(UserLocation::getUserId, Function.identity()));

		return members.stream()
				.map(member -> FamilyLocationResponse.of(member, locations.get(member.userId())))
				.toList();
	}

	// 공유를 끈 회원의 위치는 저장하지 않고 무시한다.
	@Transactional
	public void updateMyLocation(Long userId, LocationUpdateRequest request) {
		UserLocation location = getOrCreate(userId);
		if (!location.isSharingEnabled()) {
			return;
		}
		location.updateLocation(scale(request.latitude()), scale(request.longitude()), LocalDateTime.now());
	}

	@Transactional
	public LocationSharingResponse changeSharing(Long userId, LocationSharingRequest request) {
		UserLocation location = getOrCreate(userId);
		location.changeSharing(request.sharingEnabled());
		return new LocationSharingResponse(location.isSharingEnabled());
	}

	@Transactional
	public void deleteLocation(Long userId) {
		userLocationRepository.deleteById(userId);
	}

	private UserLocation getOrCreate(Long userId) {
		return userLocationRepository.findById(userId)
				.orElseGet(() -> userLocationRepository.save(new UserLocation(userService.getUser(userId))));
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}

}
