package com.urijip.server.schedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.schedule.dto.request.TodoCreateRequest;
import com.urijip.server.schedule.dto.request.TodoUpdateRequest;
import com.urijip.server.schedule.dto.response.TodoResponse;
import com.urijip.server.schedule.service.TodoService;
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

@WebMvcTest(TodoController.class)
@Import(SecurityConfig.class)
class TodoControllerTest {

	private static final LocalDate OCT_2 = LocalDate.of(2026, 10, 2);
	private static final TodoResponse TODO = new TodoResponse(7L, "장보기", 2L, OCT_2, false, 1L);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private TodoService todoService;

	@Test
	void getTodosReturnsList() {
		given(todoService.getTodos(3L, 1L, false)).willReturn(List.of(TODO));

		MvcTestResult result = mvc.get().uri("/api/families/3/todos?done=false")
				.header("Authorization", bearer(1L))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data[0].id").isEqualTo(7);
		assertThat(result).bodyJson().extractingPath("$.data[0].title").isEqualTo("장보기");
		assertThat(result).bodyJson().extractingPath("$.data[0].assigneeId").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.data[0].dueDate").isEqualTo("2026-10-02");
		assertThat(result).bodyJson().extractingPath("$.data[0].done").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.data[0].createdBy").isEqualTo(1);
	}

	@Test
	void getTodosWithoutDoneReturnsAll() {
		mvc.get().uri("/api/families/3/todos").header("Authorization", bearer(1L)).exchange();

		then(todoService).should().getTodos(3L, 1L, null);
	}

	@Test
	void getTodosWithInvalidDoneReturnsBadRequest() {
		MvcTestResult result = mvc.get().uri("/api/families/3/todos?done=maybe")
				.header("Authorization", bearer(1L))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(todoService).should(never()).getTodos(any(), any(), any());
	}

	@Test
	void getTodosOfOtherFamilyReturnsForbidden() {
		given(todoService.getTodos(any(), any(), any())).willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = mvc.get().uri("/api/families/3/todos").header("Authorization", bearer(1L)).exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void createReturnsCreatedTodo() {
		given(todoService.create(3L, 1L, new TodoCreateRequest("장보기", 2L, OCT_2))).willReturn(TODO);

		MvcTestResult result = create(1L, """
				{"title": "장보기", "assigneeId": 2, "dueDate": "2026-10-02"}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(7);
	}

	@Test
	void createAcceptsTitleOnly() {
		create(1L, """
				{"title": "청소"}
				""");

		then(todoService).should().create(3L, 1L, new TodoCreateRequest("청소", null, null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"title\": \" \"}"})
	void createWithInvalidTitleReturnsBadRequest(String body) {
		MvcTestResult result = create(1L, body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains("title: ");
		then(todoService).should(never()).create(any(), any(), any());
	}

	@Test
	void createWithTooLongTitleReturnsBadRequest() {
		MvcTestResult result = create(1L, """
				{"title": "%s"}
				""".formatted("가".repeat(51)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.message").isEqualTo("title: 50자 이하여야 합니다");
	}

	@Test
	void createWithAssigneeOutsideFamilyReturnsNotFound() {
		given(todoService.create(any(), any(), any()))
				.willThrow(new BusinessException(ErrorCode.FAMILY_MEMBER_NOT_FOUND));

		MvcTestResult result = create(1L, """
				{"title": "장보기", "assigneeId": 99}
				""");

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_MEMBER_NOT_FOUND");
	}

	@Test
	void createWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/families/3/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "장보기"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		then(todoService).should(never()).create(any(), any(), any());
	}

	@Test
	void updateWithOnlyDoneLeavesOtherFieldsUntouched() {
		update(1L, """
				{"done": true}
				""");

		then(todoService).should().update(7L, 1L, done(true));
	}

	@Test
	void updateWithNullClearsAssigneeAndDueDate() {
		update(1L, """
				{"assigneeId": null, "dueDate": null}
				""");

		then(todoService).should().update(7L, 1L,
				clearAssigneeAndDueDate());
	}

	@Test
	void updateWithValuesChangesFields() {
		given(todoService.update(7L, 1L, updateRequest("마트", 2L, OCT_2, false)))
				.willReturn(TODO);

		MvcTestResult result = update(1L, """
				{"title": "마트", "assigneeId": 2, "dueDate": "2026-10-02", "done": false}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(7);
	}

	@Test
	void updateIgnoresSentFlagsInBody() {
		update(1L, """
				{"done": true, "assigneeIdSent": true, "dueDateSent": true}
				""");

		then(todoService).should().update(7L, 1L, done(true));
	}

	@Test
	void updateWithBlankTitleReturnsBadRequest() {
		MvcTestResult result = update(1L, """
				{"title": " "}
				""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains("title: ");
		then(todoService).should(never()).update(any(), any(), any());
	}

	@Test
	void updateUnknownTodoReturnsNotFound() {
		given(todoService.update(any(), any(), any())).willThrow(new BusinessException(ErrorCode.TODO_NOT_FOUND));

		MvcTestResult result = update(1L, """
				{"done": true}
				""");

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("TODO_NOT_FOUND");
	}

	@Test
	void deleteReturnsOkWithoutData() {
		MvcTestResult result = mvc.delete().uri("/api/todos/7").header("Authorization", bearer(1L)).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data").isNull();
		then(todoService).should().delete(7L, 1L);
	}

	@Test
	void deleteOfOtherFamilyReturnsForbidden() {
		willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER)).given(todoService).delete(7L, 1L);

		MvcTestResult result = mvc.delete().uri("/api/todos/7").header("Authorization", bearer(1L)).exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	private MvcTestResult create(Long userId, String body) {
		return mvc.post().uri("/api/families/3/todos")
				.header("Authorization", bearer(userId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult update(Long userId, String body) {
		return mvc.patch().uri("/api/todos/7")
				.header("Authorization", bearer(userId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId, "USER");
	}

	private TodoUpdateRequest done(boolean done) {
		TodoUpdateRequest request = new TodoUpdateRequest();
		request.setDone(done);
		return request;
	}

	private TodoUpdateRequest assignee(Long assigneeId) {
		TodoUpdateRequest request = new TodoUpdateRequest();
		request.setAssigneeId(assigneeId);
		return request;
	}

	private TodoUpdateRequest clearAssigneeAndDueDate() {
		TodoUpdateRequest request = new TodoUpdateRequest();
		request.setAssigneeId(null);
		request.setDueDate(null);
		return request;
	}

	private TodoUpdateRequest updateRequest(String title, Long assigneeId, LocalDate dueDate, boolean done) {
		TodoUpdateRequest request = new TodoUpdateRequest();
		request.setTitle(title);
		request.setAssigneeId(assigneeId);
		request.setDueDate(dueDate);
		request.setDone(done);
		return request;
	}

}
