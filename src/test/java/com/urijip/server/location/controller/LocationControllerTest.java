package com.urijip.server.location.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.location.dto.request.LocationSharingRequest;
import com.urijip.server.location.dto.request.LocationUpdateRequest;
import com.urijip.server.location.dto.response.FamilyLocationResponse;
import com.urijip.server.location.dto.response.LocationSharingResponse;
import com.urijip.server.location.service.LocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(LocationController.class)
@Import(SecurityConfig.class)
class LocationControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private LocationService locationService;

	@Test
	void getFamilyLocationsReturnsMembers() {
		given(locationService.getFamilyLocations(3L, 1L)).willReturn(List.of(
				new FamilyLocationResponse(1L, "김엄마", "엄마", null, true, 37.5665, 126.978,
						OffsetDateTime.of(2026, 10, 17, 14, 0, 0, 0, ZoneOffset.ofHours(9))),
				new FamilyLocationResponse(2L, "김아빠", null, null, false, null, null, null)));

		MvcTestResult result = mvc.get().uri("/api/families/3/locations").header("Authorization", bearer(1L))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data[0].userId").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data[0].nickname").isEqualTo("엄마");
		assertThat(result).bodyJson().extractingPath("$.data[0].sharingEnabled").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data[0].latitude").isEqualTo(37.5665);
		assertThat(result).bodyJson().extractingPath("$.data[0].longitude").isEqualTo(126.978);
		assertThat(result).bodyJson().extractingPath("$.data[0].updatedAt").isEqualTo("2026-10-17T14:00:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data[1].sharingEnabled").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.data[1].latitude").isNull();
	}

	@Test
	void getFamilyLocationsOfOtherFamilyReturnsForbidden() {
		given(locationService.getFamilyLocations(any(), any()))
				.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = mvc.get().uri("/api/families/3/locations").header("Authorization", bearer(9L))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void getFamilyLocationsWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/api/families/3/locations").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void updateMyLocationReturnsOk() {
		MvcTestResult result = put("/api/users/me/location", """
				{"latitude": 37.5665, "longitude": 126.978}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		then(locationService).should().updateMyLocation(1L,
				new LocationUpdateRequest(new BigDecimal("37.5665"), new BigDecimal("126.978")));
	}

	@Test
	void updateMyLocationAcceptsBoundaryValues() {
		MvcTestResult result = put("/api/users/me/location", """
				{"latitude": -90, "longitude": 180}
				""");

		assertThat(result).hasStatusOk();
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"latitude\": 37.5}", "{\"longitude\": 126.9}",
			"{\"latitude\": 90.1, \"longitude\": 126.9}", "{\"latitude\": -90.1, \"longitude\": 126.9}",
			"{\"latitude\": 37.5, \"longitude\": 180.1}", "{\"latitude\": 37.5, \"longitude\": -180.1}"})
	void updateMyLocationWithInvalidCoordinatesReturnsBadRequest(String body) {
		MvcTestResult result = put("/api/users/me/location", body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(locationService).should(never()).updateMyLocation(any(), any());
	}

	@Test
	void updateMyLocationWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.put().uri("/api/users/me/location")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"latitude\": 37.5, \"longitude\": 126.9}")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void changeSharingReturnsCurrentSetting() {
		given(locationService.changeSharing(1L, new LocationSharingRequest(false)))
				.willReturn(new LocationSharingResponse(false));

		MvcTestResult result = mvc.patch().uri("/api/users/me/location-sharing")
				.header("Authorization", bearer(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"sharingEnabled\": false}")
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.sharingEnabled").isEqualTo(false);
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"sharingEnabled\": null}"})
	void changeSharingWithoutValueReturnsBadRequest(String body) {
		MvcTestResult result = mvc.patch().uri("/api/users/me/location-sharing")
				.header("Authorization", bearer(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(locationService).should(never()).changeSharing(any(), any());
	}

	private MvcTestResult put(String uri, String body) {
		return mvc.put().uri(uri)
				.header("Authorization", bearer(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId, "USER");
	}

}
