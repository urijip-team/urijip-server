package com.urijip.server.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void businessExceptionUsesStatusAndMessageOfErrorCode() {
		MvcTestResult result = mvc.get().uri("/test/business").exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.message").isEqualTo(ErrorCode.INVALID_INPUT.getMessage());
		assertThat(result).bodyJson().extractingPath("$.errors").asArray().isEmpty();
	}

	@Test
	void validationFailureListsInvalidFields() {
		MvcTestResult result = mvc.post().uri("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": ""}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");
		assertThat(result).bodyJson().extractingPath("$.errors[0].message").asString().isNotBlank();
	}

	@Test
	void unexpectedExceptionHidesInternalMessage() {
		MvcTestResult result = mvc.get().uri("/test/unexpected").exchange();

		assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INTERNAL_SERVER_ERROR");
		assertThat(result).bodyJson().extractingPath("$.message")
				.isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
	}

	@Test
	void frameworkExceptionKeepsStatusInCommonFormat() {
		MvcTestResult result = mvc.post().uri("/test/business").exchange();

		assertThat(result).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("METHOD_NOT_ALLOWED");
	}

	@RestController
	static class TestController {

		@GetMapping("/test/business")
		void business() {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}

		@PostMapping("/test/validation")
		void validation(@Valid @RequestBody TestRequest request) {
		}

		@GetMapping("/test/unexpected")
		void unexpected() {
			throw new IllegalStateException("internal detail");
		}

	}

	record TestRequest(@NotBlank String name) {
	}

}
