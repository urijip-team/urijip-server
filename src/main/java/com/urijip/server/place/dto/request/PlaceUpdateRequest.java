package com.urijip.server.place.dto.request;

import java.math.BigDecimal;

import com.urijip.server.place.entity.Place;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 보낸 항목만 바꾼다. 위도·경도는 함께 보내야 한다.
public record PlaceUpdateRequest(
		@Pattern(regexp = ".*\\S.*") @Size(max = Place.MAX_NAME_LENGTH) String name,
		@DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
		@Min(Place.MIN_RADIUS_METERS) @Max(Place.MAX_RADIUS_METERS) Integer radiusMeters) {

	@AssertTrue(message = "위도와 경도는 함께 보내야 합니다")
	public boolean isCoordinatesPaired() {
		return (latitude == null) == (longitude == null);
	}

}
