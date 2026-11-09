package com.urijip.server.place.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.place.entity.Place;

public record PlaceResponse(Long id, String name, double latitude, double longitude, int radiusMeters,
		OffsetDateTime createdAt) {

	public static PlaceResponse from(Place place) {
		return new PlaceResponse(place.getId(), place.getName(), place.getLatitude().doubleValue(),
				place.getLongitude().doubleValue(), place.getRadiusMeters(), SeoulTime.toOffset(place.getCreatedAt()));
	}

}
