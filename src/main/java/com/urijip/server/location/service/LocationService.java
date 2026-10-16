package com.urijip.server.location.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.location.dto.request.LocationSendRequest;
import com.urijip.server.location.dto.request.LocationSharingRequest;
import com.urijip.server.location.dto.request.LocationUpdateRequest;
import com.urijip.server.location.dto.response.FamilyLocationResponse;
import com.urijip.server.location.dto.response.LocationChangedMessage;
import com.urijip.server.location.dto.response.LocationSharingResponse;
import com.urijip.server.location.entity.UserLocation;
import com.urijip.server.location.repository.UserLocationRepository;
import com.urijip.server.member.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
	private final ApplicationEventPublisher eventPublisher;

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

	// 공유를 끈 회원의 위치는 저장하지 않고 무시한다. 저장하면 가족에게 실시간으로 알린다.
	@Transactional
	public void updateMyLocation(Long userId, LocationUpdateRequest request) {
		UserLocation location = getOrCreate(userId);
		if (!location.isSharingEnabled()) {
			return;
		}
		location.updateLocation(scale(request.latitude()), scale(request.longitude()), LocalDateTime.now());
		publishChanged(location);
	}

	// STOMP /pub/location. 본문의 가족에 속하지 않으면 FAMILY_NOT_MEMBER.
	@Transactional
	public void sendLocation(Long userId, LocationSendRequest request) {
		familyService.validateMember(request.familyId(), userId);
		updateMyLocation(userId, request.toUpdateRequest());
	}

	@Transactional
	public LocationSharingResponse changeSharing(Long userId, LocationSharingRequest request) {
		UserLocation location = getOrCreate(userId);
		location.changeSharing(request.sharingEnabled());
		publishChanged(location);
		return new LocationSharingResponse(location.isSharingEnabled());
	}

	@Transactional
	public void deleteLocation(Long userId) {
		userLocationRepository.deleteById(userId);
	}

	private void publishChanged(UserLocation location) {
		familyService.findFamilyOf(location.getUserId())
				.map(FamilyResponse::id)
				.ifPresent(familyId -> eventPublisher.publishEvent(
						new LocationChangedEvent(familyId, LocationChangedMessage.from(location))));
	}

	private UserLocation getOrCreate(Long userId) {
		return userLocationRepository.findById(userId)
				.orElseGet(() -> userLocationRepository.save(new UserLocation(userService.getUser(userId))));
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}

}
