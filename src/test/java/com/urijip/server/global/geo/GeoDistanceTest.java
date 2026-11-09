package com.urijip.server.global.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoDistanceTest {

	@Test
	void samePointIsZero() {
		assertThat(GeoDistance.meters(35.1595, 126.8526, 35.1595, 126.8526)).isZero();
	}

	@Test
	void oneThousandthDegreeOfLatitudeIsAboutHundredElevenMeters() {
		assertThat(GeoDistance.meters(35.1595, 126.8526, 35.1605, 126.8526)).isCloseTo(111.2, within(0.5));
	}

	@Test
	void seoulToBusanIsAboutThreeHundredTwentyFiveKilometers() {
		assertThat(GeoDistance.meters(37.5665, 126.9780, 35.1796, 129.0756)).isCloseTo(325_000, within(2_000.0));
	}

}
