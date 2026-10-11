package com.urijip.server.family.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.security.JwtProvider;
import org.junit.jupiter.api.Test;
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

	private MvcTestResult create(Long userId, String body) {
		return mvc.post().uri("/api/families")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

}
