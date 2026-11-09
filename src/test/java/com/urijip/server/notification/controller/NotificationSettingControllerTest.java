package com.urijip.server.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.notification.dto.request.NotificationSettingUpdateRequest;
import com.urijip.server.notification.dto.response.NotificationSettingResponse;
import com.urijip.server.notification.service.NotificationSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(NotificationSettingController.class)
@Import(SecurityConfig.class)
class NotificationSettingControllerTest {

	private static final String URL = "/api/users/me/notification-settings";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private NotificationSettingService notificationSettingService;

	@Test
	void getReturnsMySettings() {
		given(notificationSettingService.getSettings(1L))
				.willReturn(new NotificationSettingResponse(false, true, true, false, true));

		MvcTestResult result = mvc.get().uri(URL).header("Authorization", bearer()).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.chat").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.data.schedule").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.allowance").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.notice").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.data.place").isEqualTo(true);
	}

	@Test
	void getWithoutTokenIsUnauthorized() {
		assertThat(mvc.get().uri(URL).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void patchChangesOnlySentFields() {
		given(notificationSettingService.update(1L, new NotificationSettingUpdateRequest(false, null, null, null, null)))
				.willReturn(new NotificationSettingResponse(false, true, true, true, true));

		MvcTestResult result = mvc.patch().uri(URL).header("Authorization", bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"chat": false}
						""")
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.chat").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.data.notice").isEqualTo(true);
	}

	@Test
	void patchWithNonBooleanReturnsBadRequest() {
		MvcTestResult result = mvc.patch().uri(URL).header("Authorization", bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"chat": "yes"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	private String bearer() {
		return "Bearer " + jwtProvider.createAccessToken(1L, "USER");
	}

}
