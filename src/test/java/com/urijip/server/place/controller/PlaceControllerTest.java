package com.urijip.server.place.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.place.dto.request.PlaceCreateRequest;
import com.urijip.server.place.dto.response.PlaceResponse;
import com.urijip.server.place.service.PlaceService;
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

@WebMvcTest(PlaceController.class)
@Import(SecurityConfig.class)
class PlaceControllerTest {

	private static final PlaceResponse HOME = new PlaceResponse(5L, "집", 35.1595, 126.8526, 100,
			OffsetDateTime.parse("2026-11-09T10:00:00+09:00"));

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private PlaceService placeService;

	@Test
	void getPlacesReturnsList() {
		given(placeService.getPlaces(3L, 2L)).willReturn(List.of(HOME));

		MvcTestResult result = mvc.get().uri("/api/families/3/places").header("Authorization", bearer()).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data[0].id").isEqualTo(5);
		assertThat(result).bodyJson().extractingPath("$.data[0].name").isEqualTo("집");
		assertThat(result).bodyJson().extractingPath("$.data[0].latitude").isEqualTo(35.1595);
		assertThat(result).bodyJson().extractingPath("$.data[0].radiusMeters").isEqualTo(100);
		assertThat(result).bodyJson().extractingPath("$.data[0].createdAt").isEqualTo("2026-11-09T10:00:00+09:00");
	}

	@Test
	void createReturnsCreated() {
		given(placeService.create(3L, 2L, new PlaceCreateRequest("집", new BigDecimal("35.1595"),
				new BigDecimal("126.8526"), null))).willReturn(HOME);

		MvcTestResult result = post("""
				{"name": "집", "latitude": 35.1595, "longitude": 126.8526}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(5);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"latitude\": 35.1, \"longitude\": 126.8}",
			"{\"name\": \" \", \"latitude\": 35.1, \"longitude\": 126.8}",
			"{\"name\": \"집\", \"longitude\": 126.8}",
			"{\"name\": \"집\", \"latitude\": 91, \"longitude\": 126.8}",
			"{\"name\": \"집\", \"latitude\": 35.1, \"longitude\": 126.8, \"radiusMeters\": 49}",
			"{\"name\": \"집\", \"latitude\": 35.1, \"longitude\": 126.8, \"radiusMeters\": 501}"
	})
	void createWithInvalidBodyReturnsBadRequest(String body) {
		MvcTestResult result = post(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(placeService).should(never()).create(any(), any(), any());
	}

	private MvcTestResult post(String body) {
		return mvc.post().uri("/api/families/3/places").header("Authorization", bearer())
				.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private String bearer() {
		return "Bearer " + jwtProvider.createAccessToken(2L, "USER");
	}

}
