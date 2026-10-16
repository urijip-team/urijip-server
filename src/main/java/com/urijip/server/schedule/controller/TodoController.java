package com.urijip.server.schedule.controller;

import java.util.List;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.schedule.dto.request.TodoCreateRequest;
import com.urijip.server.schedule.dto.request.TodoUpdateRequest;
import com.urijip.server.schedule.dto.response.TodoResponse;
import com.urijip.server.schedule.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TodoController {

	private final TodoService todoService;

	@GetMapping("/api/families/{familyId}/todos")
	public ApiResponse<List<TodoResponse>> getTodos(@AuthenticationPrincipal Long userId, @PathVariable Long familyId,
			@RequestParam(required = false) Boolean done) {
		return ApiResponse.success(todoService.getTodos(familyId, userId, done));
	}

	@PostMapping("/api/families/{familyId}/todos")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<TodoResponse> create(@AuthenticationPrincipal Long userId, @PathVariable Long familyId,
			@Valid @RequestBody TodoCreateRequest request) {
		return ApiResponse.success(todoService.create(familyId, userId, request));
	}

	@PatchMapping("/api/todos/{todoId}")
	public ApiResponse<TodoResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long todoId,
			@Valid @RequestBody TodoUpdateRequest request) {
		return ApiResponse.success(todoService.update(todoId, userId, request));
	}

	@DeleteMapping("/api/todos/{todoId}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long todoId) {
		todoService.delete(todoId, userId);
		return ApiResponse.success(null);
	}

}
