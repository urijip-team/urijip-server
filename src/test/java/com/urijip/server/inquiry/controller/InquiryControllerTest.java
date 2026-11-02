package com.urijip.server.inquiry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.inquiry.dto.request.InquiryCreateRequest;
import com.urijip.server.inquiry.dto.response.InquiryResponse;
import com.urijip.server.inquiry.service.InquiryService;
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

@WebMvcTest(InquiryController.class)
@Import(SecurityConfig.class)
class InquiryControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private InquiryService inquiryService;

	@Test
	void createReturnsCreated() {
		given(inquiryService.create(1L, new InquiryCreateRequest("로그인이 안 돼요", "비밀번호가 맞는데 실패합니다.")))
				.willReturn(new InquiryResponse(7L, "로그인이 안 돼요", "비밀번호가 맞는데 실패합니다.", null, null));

		MvcTestResult result = create("""
				{"title": "로그인이 안 돼요", "content": "비밀번호가 맞는데 실패합니다."}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(7);
		assertThat(result).bodyJson().extractingPath("$.data.title").isEqualTo("로그인이 안 돼요");
		assertThat(result).bodyJson().extractingPath("$.data.content").isEqualTo("비밀번호가 맞는데 실패합니다.");
		assertThat(result).bodyJson().extractingPath("$.data.answer").isNull();
		assertThat(result).bodyJson().extractingPath("$.data.answeredAt").isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"content\": \"내용\"}",
			"{\"title\": \"제목\"}",
			"{\"title\": \" \", \"content\": \"내용\"}",
			"{\"title\": \"제목\", \"content\": \" \"}"
	})
	void createWithInvalidBodyReturnsBadRequest(String body) {
		MvcTestResult result = create(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(inquiryService).should(never()).create(any(), any());
	}

	@Test
	void createWithTooLongTitleReturnsBadRequest() {
		MvcTestResult result = create("{\"title\": \"%s\", \"content\": \"내용\"}".formatted("가".repeat(101)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createWithTooLongContentReturnsBadRequest() {
		MvcTestResult result = create("{\"title\": \"제목\", \"content\": \"%s\"}".formatted("가".repeat(2001)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/inquiries")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\": \"제목\", \"content\": \"내용\"}")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MvcTestResult create(String body) {
		return mvc.post().uri("/api/inquiries")
				.header("Authorization", user())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String user() {
		return "Bearer " + jwtProvider.createAccessToken(1L, "USER");
	}

}
