package com.urijip.server.global.geo;

// 두 좌표 사이의 지표면 거리(m). 하버사인 공식, 지구 반지름 6,371km. 수백 m 안의 판단에는 충분히 정확하다.
public final class GeoDistance {

	private static final double EARTH_RADIUS_METERS = 6_371_000;

	private GeoDistance() {
	}

	public static double meters(double latitude1, double longitude1, double latitude2, double longitude2) {
		double dLat = Math.toRadians(latitude2 - latitude1);
		double dLon = Math.toRadians(longitude2 - longitude1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

}
