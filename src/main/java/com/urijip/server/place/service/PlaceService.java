package com.urijip.server.place.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.urijip.server.family.service.FamilyDeletedEvent;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.service.UserService;
import com.urijip.server.place.dto.request.PlaceCreateRequest;
import com.urijip.server.place.dto.request.PlaceUpdateRequest;
import com.urijip.server.place.dto.response.PlaceResponse;
import com.urijip.server.place.entity.Place;
import com.urijip.server.place.repository.PlacePresenceRepository;
import com.urijip.server.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceService {

	// DB 컬럼 decimal(10, 7)에 맞춘다.
	private static final int SCALE = 7;

	private final PlaceRepository placeRepository;
	private final PlacePresenceRepository placePresenceRepository;
	private final FamilyService familyService;
	private final UserService userService;

	// 멤버가 아니면 FAMILY_NOT_MEMBER, 이미 10개면 PLACE_LIMIT_EXCEEDED.
	@Transactional
	public PlaceResponse create(Long familyId, Long userId, PlaceCreateRequest request) {
		familyService.validateActiveMember(familyId, userId);
		if (placeRepository.countByFamilyId(familyId) >= Place.MAX_PLACES_PER_FAMILY) {
			throw new BusinessException(ErrorCode.PLACE_LIMIT_EXCEEDED);
		}

		Place place = placeRepository.save(new Place(familyService.getFamily(familyId), userService.getUser(userId),
				request.name(), scale(request.latitude()), scale(request.longitude()), request.radiusOrDefault()));
		return PlaceResponse.from(place);
	}

	@Transactional(readOnly = true)
	public List<PlaceResponse> getPlaces(Long familyId, Long userId) {
		familyService.validateActiveMember(familyId, userId);
		return placeRepository.findByFamilyIdOrderByIdAsc(familyId).stream().map(PlaceResponse::from).toList();
	}

	// 같은 가족 멤버면 누구나 바꿀 수 있다.
	@Transactional
	public PlaceResponse update(Long placeId, Long userId, PlaceUpdateRequest request) {
		Place place = getAccessiblePlace(placeId, userId);
		place.update(request.name(), scaleOrNull(request.latitude()), scaleOrNull(request.longitude()),
				request.radiusMeters());
		return PlaceResponse.from(place);
	}

	// 장소 안에 있다는 기록이 장소를 참조하므로 먼저 지운다.
	@Transactional
	public void delete(Long placeId, Long userId) {
		Place place = getAccessiblePlace(placeId, userId);
		placePresenceRepository.deleteByPlaceId(placeId);
		placeRepository.delete(place);
	}

	@EventListener
	public void onFamilyDeleted(FamilyDeletedEvent event) {
		placePresenceRepository.deleteByFamilyId(event.familyId());
		placeRepository.deleteByFamilyId(event.familyId());
	}

	// 장소가 없으면 PLACE_NOT_FOUND, 그 가족의 멤버가 아니면 FAMILY_NOT_MEMBER.
	private Place getAccessiblePlace(Long placeId, Long userId) {
		Place place = placeRepository.findById(placeId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
		familyService.validateActiveMember(place.getFamily().getId(), userId);
		return place;
	}

	private BigDecimal scaleOrNull(BigDecimal value) {
		return value == null ? null : scale(value);
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}

}
