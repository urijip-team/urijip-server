package com.urijip.server.place.service;

import java.util.Set;

import com.urijip.server.global.geo.GeoDistance;
import com.urijip.server.location.dto.response.LocationChangedMessage;
import com.urijip.server.location.service.LocationChangedEvent;
import com.urijip.server.member.service.UserService;
import com.urijip.server.place.entity.Place;
import com.urijip.server.place.entity.PlacePresence;
import com.urijip.server.place.repository.PlacePresenceRepository;
import com.urijip.server.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 위치가 바뀔 때마다 가족 장소에 들어왔는지·나갔는지 본다(LOC-05). 위치 저장과 같은 트랜잭션에서 돈다.
 * 반경 안에 들어오면 도착, 반경보다 LEAVE_MARGIN_METERS 더 멀어지면 출발이다. 경계에서 GPS가 흔들려도 되풀이되지 않게 차이를 둔다.
 * 위치 공유를 끈 회원은 위치가 오지 않으므로 감지하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class PlaceAlertService {

	static final int LEAVE_MARGIN_METERS = 30;

	private final PlaceRepository placeRepository;
	private final PlacePresenceRepository placePresenceRepository;
	private final UserService userService;

	@EventListener
	public void onLocationChanged(LocationChangedEvent event) {
		LocationChangedMessage location = event.message();
		if (!location.sharingEnabled() || location.latitude() == null) {
			return;
		}
		Long userId = location.userId();
		Set<Long> inside = Set.copyOf(placePresenceRepository.findPlaceIdsByUserId(userId));

		for (Place place : placeRepository.findByFamilyIdOrderByIdAsc(event.familyId())) {
			double distance = GeoDistance.meters(place.getLatitude().doubleValue(), place.getLongitude().doubleValue(),
					location.latitude(), location.longitude());
			boolean wasInside = inside.contains(place.getId());
			if (!wasInside && distance <= place.getRadiusMeters()) {
				placePresenceRepository.save(new PlacePresence(place, userService.getUser(userId)));
			} else if (wasInside && distance > place.getRadiusMeters() + LEAVE_MARGIN_METERS) {
				placePresenceRepository.deleteByPlaceIdAndUserId(place.getId(), userId);
			}
		}
	}

}
