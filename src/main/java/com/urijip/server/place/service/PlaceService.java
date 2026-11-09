package com.urijip.server.place.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.service.UserService;
import com.urijip.server.place.dto.request.PlaceCreateRequest;
import com.urijip.server.place.dto.response.PlaceResponse;
import com.urijip.server.place.entity.Place;
import com.urijip.server.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceService {

	// DB 컬럼 decimal(10, 7)에 맞춘다.
	private static final int SCALE = 7;

	private final PlaceRepository placeRepository;
	private final FamilyService familyService;
	private final UserService userService;

	// 멤버가 아니면 FAMILY_NOT_MEMBER, 이미 10개면 PLACE_LIMIT_EXCEEDED.
	@Transactional
	public PlaceResponse create(Long familyId, Long userId, PlaceCreateRequest request) {
		familyService.validateMember(familyId, userId);
		if (placeRepository.countByFamilyId(familyId) >= Place.MAX_PLACES_PER_FAMILY) {
			throw new BusinessException(ErrorCode.PLACE_LIMIT_EXCEEDED);
		}

		Place place = placeRepository.save(new Place(familyService.getFamily(familyId), userService.getUser(userId),
				request.name(), scale(request.latitude()), scale(request.longitude()), request.radiusOrDefault()));
		return PlaceResponse.from(place);
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}

}
