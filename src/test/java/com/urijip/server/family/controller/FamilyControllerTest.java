package com.urijip.server.family.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
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

@WebMvcTest(FamilyController.class)
@Import(SecurityConfig.class)
class FamilyControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private FamilyService familyService;

	@Test
	void createReturnsCreatedWithFamily() {
		given(familyService.create(1L, new FamilyCreateRequest("김씨네")))
				.willReturn(new FamilyResponse(3L, "김씨네", "A3K9P2"));

		MvcTestResult result = create(1L, """
				{"name": "김씨네"}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo("김씨네");
		assertThat(result).bodyJson().extractingPath("$.data.inviteCode").isEqualTo("A3K9P2");
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	@Test
	void createPassesUserIdOfAccessToken() {
		create(7L, """
				{"name": "김씨네"}
				""");

		then(familyService).should().create(7L, new FamilyCreateRequest("김씨네"));
	}

	@Test
	void createWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/families")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "김씨네"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(familyService).should(never()).create(any(), any());
	}

	@Test
	void createByUserWhoAlreadyHasFamilyReturnsConflict() {
		given(familyService.create(any(), any()))
				.willThrow(new BusinessException(ErrorCode.ALREADY_FAMILY_MEMBER));

		MvcTestResult result = create(1L, """
				{"name": "김씨네"}
				""");

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("ALREADY_FAMILY_MEMBER");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"name\": \"\"}", "{\"name\": \" \"}"})
	void createWithoutNameReturnsBadRequest(String body) {
		MvcTestResult result = create(1L, body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains("name: ");
		then(familyService).should(never()).create(any(), any());
	}

	@Test
	void createWithNameLongerThanTwentyCharactersReturnsBadRequest() {
		MvcTestResult result = create(1L, "{\"name\": \"" + "가".repeat(21) + "\"}");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.message").isEqualTo("name: 20자 이하여야 합니다");
		then(familyService).should(never()).create(any(), any());
	}

	@Test
	void createWithTwentyCharacterNameIsAccepted() {
		MvcTestResult result = create(1L, "{\"name\": \"" + "가".repeat(20) + "\"}");

		assertThat(result).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void joinReturnsOkWithFamily() {
		given(familyService.join(2L, new FamilyJoinRequest("A3K9P2")))
				.willReturn(new FamilyResponse(3L, "김씨네", "A3K9P2"));

		MvcTestResult result = join(2L, """
				{"inviteCode": "A3K9P2"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo("김씨네");
		assertThat(result).bodyJson().extractingPath("$.data.inviteCode").isEqualTo("A3K9P2");
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	private MvcTestResult create(Long userId, String body) {
		return mvc.post().uri("/api/families")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult join(Long userId, String body) {
		return mvc.post().uri("/api/families/join")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

}
