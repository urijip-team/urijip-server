package com.urijip.server.schedule.dto.response;

import java.time.LocalDate;

import com.urijip.server.schedule.entity.Todo;

public record TodoResponse(Long id, String title, Long assigneeId, LocalDate dueDate, boolean done, Long createdBy) {

	public static TodoResponse from(Todo todo) {
		Long assigneeId = todo.getAssignee() == null ? null : todo.getAssignee().getId();
		return new TodoResponse(todo.getId(), todo.getTitle(), assigneeId, todo.getDueDate(), todo.isDone(),
				todo.getCreatedBy().getId());
	}

}
