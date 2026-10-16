package com.urijip.server.schedule.service;

import java.util.List;

import com.urijip.server.family.service.FamilyDeletedEvent;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.service.UserService;
import com.urijip.server.schedule.dto.request.TodoCreateRequest;
import com.urijip.server.schedule.dto.request.TodoUpdateRequest;
import com.urijip.server.schedule.dto.response.TodoResponse;
import com.urijip.server.schedule.entity.Todo;
import com.urijip.server.schedule.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoService {

	private final TodoRepository todoRepository;
	private final FamilyService familyService;
	private final UserService userService;

	@Transactional
	public TodoResponse create(Long familyId, Long userId, TodoCreateRequest request) {
		familyService.validateMember(familyId, userId);

		Todo todo = todoRepository.save(Todo.builder()
				.family(familyService.getFamily(familyId))
				.title(request.title())
				.assignee(findAssignee(familyId, request.assigneeId()))
				.dueDate(request.dueDate())
				.createdBy(userService.getUser(userId))
				.build());

		return TodoResponse.from(todo);
	}

	@Transactional(readOnly = true)
	public List<TodoResponse> getTodos(Long familyId, Long userId, Boolean done) {
		familyService.validateMember(familyId, userId);

		return todoRepository.findByFamily(familyId, done).stream()
				.map(TodoResponse::from)
				.toList();
	}

	@Transactional
	public TodoResponse update(Long todoId, Long userId, TodoUpdateRequest request) {
		Todo todo = getAccessibleTodo(todoId, userId);

		if (request.getTitle() != null) {
			todo.changeTitle(request.getTitle());
		}
		if (request.isAssigneeIdSent()) {
			todo.changeAssignee(findAssignee(todo.getFamily().getId(), request.getAssigneeId()));
		}
		if (request.isDueDateSent()) {
			todo.changeDueDate(request.getDueDate());
		}
		if (request.getDone() != null) {
			todo.changeDone(request.getDone());
		}

		return TodoResponse.from(todo);
	}

	@Transactional
	public void delete(Long todoId, Long userId) {
		todoRepository.delete(getAccessibleTodo(todoId, userId));
	}

	@EventListener
	public void onFamilyDeleted(FamilyDeletedEvent event) {
		todoRepository.deleteByFamilyId(event.familyId());
	}

	private Todo getAccessibleTodo(Long todoId, Long userId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));
		familyService.validateMember(todo.getFamily().getId(), userId);
		return todo;
	}

	private User findAssignee(Long familyId, Long assigneeId) {
		if (assigneeId == null) {
			return null;
		}
		familyService.validateMembers(familyId, List.of(assigneeId));
		return userService.getUser(assigneeId);
	}

}
