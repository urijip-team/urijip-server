package com.urijip.server.place.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.geo.GeoDistance;
import com.urijip.server.global.push.PushMessage;
import com.urijip.server.global.push.PushType;
import com.urijip.server.location.dto.response.LocationChangedMessage;
import com.urijip.server.location.service.LocationChangedEvent;
import com.urijip.server.member.service.UserService;
import com.urijip.server.notification.service.NotificationService;
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
 * 위치 공유를 끈 회원은 위치가 오지 않으므로 감지하지 않는다. 도착·출발하면 본인을 뺀 가족에게 푸시한다.
 */
@Service
@RequiredArgsConstructor
public class PlaceAlertService {

	static final int LEAVE_MARGIN_METERS = 30;

	private final PlaceRepository placeRepository;
	private final PlacePresenceRepository placePresenceRepository;
	private final UserService userService;
	private final FamilyService familyService;
	private final NotificationService notificationService;

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
				push(event.familyId(), userId, place, "%s님이 %s에 도착했어요.");
			} else if (wasInside && distance > place.getRadiusMeters() + LEAVE_MARGIN_METERS) {
				placePresenceRepository.deleteByPlaceIdAndUserId(place.getId(), userId);
				push(event.familyId(), userId, place, "%s님이 %s에서 출발했어요.");
			}
		}
	}

	// 본인을 뺀 가족에게 보낸다. 이름은 가족 안 호칭을 먼저 쓴다.
	private void push(Long familyId, Long userId, Place place, String bodyFormat) {
		Map<Long, String> names = familyService.getMembers(familyId, userId).stream()
				.collect(Collectors.toMap(FamilyMemberResponse::userId,
						member -> member.nickname() == null ? member.name() : member.nickname()));
		Set<Long> receivers = names.keySet().stream()
				.filter(memberId -> !memberId.equals(userId))
				.collect(Collectors.toSet());
		notificationService.send(receivers, new PushMessage(PushType.PLACE, place.getName(),
				bodyFormat.formatted(names.get(userId), place.getName()),
				Map.of("placeId", place.getId().toString(), "userId", userId.toString())));
	}

}
