package com.urijip.server.inquiry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.inquiry.dto.request.AdminInquirySearchRequest;
import com.urijip.server.inquiry.dto.response.AdminInquiryResponse;
import com.urijip.server.inquiry.service.AdminInquiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(AdminInquiryController.class)
@Import(SecurityConfig.class)
class AdminInquiryControllerTest {

	private static final AdminInquiryResponse UNANSWERED = new AdminInquiryResponse(7L,
			new AdminInquiryResponse.Author(1L, "mom@example.com", "김엄마"), "로그인이 안 돼요", "내용", null, null, null);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AdminInquiryService adminInquiryService;

	@Test
	void searchReturnsPage() {
		given(adminInquiryService.search(new AdminInquirySearchRequest(false, 1, 10)))
				.willReturn(new PageResponse<>(List.of(UNANSWERED), 1, 10, 11, 2));

		MvcTestResult result = mvc.get().uri("/admin/inquiries?answered=false&page=1&size=10")
				.header("Authorization", admin())
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.content[0].id").isEqualTo(7);
		assertThat(result).bodyJson().extractingPath("$.data.content[0].user.email").isEqualTo("mom@example.com");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].title").isEqualTo("로그인이 안 돼요");
		assertThat(result).bodyJson().extractingPath("$.data.content[0].answer").isNull();
		assertThat(result).bodyJson().extractingPath("$.data.totalElements").isEqualTo(11);
	}

	@Test
	void searchUsesDefaults() {
		given(adminInquiryService.search(new AdminInquirySearchRequest(null, null, null)))
				.willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		MvcTestResult result = mvc.get().uri("/admin/inquiries").header("Authorization", admin()).exchange();

		assertThat(result).hasStatusOk();
	}

	@Test
	void searchWithInvalidAnsweredReturnsBadRequest() {
		MvcTestResult result = mvc.get().uri("/admin/inquiries?answered=maybe")
				.header("Authorization", admin())
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void searchByUserIsForbidden() {
		MvcTestResult result = mvc.get().uri("/admin/inquiries")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L, "USER"))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void searchWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/admin/inquiries").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private String admin() {
		return "Bearer " + jwtProvider.createAccessToken(100L, "ADMIN");
	}

}
