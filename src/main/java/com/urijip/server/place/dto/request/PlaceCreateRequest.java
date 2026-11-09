package com.urijip.server.place.dto.request;

import java.math.BigDecimal;

import com.urijip.server.place.entity.Place;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// radiusMeters는 50~500m, 빠지면 100m.
public record PlaceCreateRequest(
		@NotBlank @Size(max = Place.MAX_NAME_LENGTH) String name,
		@NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
		@Min(Place.MIN_RADIUS_METERS) @Max(Place.MAX_RADIUS_METERS) Integer radiusMeters) {

	public int radiusOrDefault() {
		return radiusMeters == null ? Place.DEFAULT_RADIUS_METERS : radiusMeters;
	}

}
