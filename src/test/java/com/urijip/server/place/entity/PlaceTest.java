package com.urijip.server.place.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.urijip.server.member.entity.User;
import org.junit.jupiter.api.Test;

class PlaceTest {

	private final User mom = User.builder().email("mom@example.com").password("pw").name("김엄마").build();

	@Test
	void updateChangesOnlySentValues() {
		Place place = new Place(null, mom, "집", new BigDecimal("35.1"), new BigDecimal("126.8"), 100);

		place.update(null, null, null, 200);
		assertThat(place.getName()).isEqualTo("집");
		assertThat(place.getLatitude()).isEqualByComparingTo("35.1");
		assertThat(place.getRadiusMeters()).isEqualTo(200);

		place.update("새 집", new BigDecimal("35.2"), new BigDecimal("126.9"), null);
		assertThat(place.getName()).isEqualTo("새 집");
		assertThat(place.getLatitude()).isEqualByComparingTo("35.2");
		assertThat(place.getLongitude()).isEqualByComparingTo("126.9");
		assertThat(place.getRadiusMeters()).isEqualTo(200);
	}

	@Test
	void updateIgnoresHalfOfCoordinates() {
		Place place = new Place(null, mom, "집", new BigDecimal("35.1"), new BigDecimal("126.8"), 100);

		place.update(null, new BigDecimal("35.2"), null, null);

		assertThat(place.getLatitude()).isEqualByComparingTo("35.1");
	}

}
