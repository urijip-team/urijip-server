package com.urijip.server.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.schedule.dto.request.TodoCreateRequest;
import com.urijip.server.schedule.dto.request.TodoUpdateRequest;
import com.urijip.server.schedule.dto.response.TodoResponse;
import com.urijip.server.schedule.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TodoServiceTest {

	private static final LocalDate OCT_2 = LocalDate.of(2026, 10, 2);

	@Autowired
	private TodoService todoService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private TodoRepository todoRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User dad;

	private User stranger;

	private Long familyId;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		dad = saveUser("dad@example.com", "김아빠");
		stranger = saveUser("stranger@example.com", "남");
		var family = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		familyService.join(dad.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyId = family.id();
	}

	@Test
	void createReturnsOpenTodo() {
		TodoResponse response = todoService.create(familyId, mom.getId(),
				new TodoCreateRequest("장보기", dad.getId(), OCT_2));

		assertThat(response.id()).isNotNull();
		assertThat(response).isEqualTo(new TodoResponse(response.id(), "장보기", dad.getId(), OCT_2, false,
				mom.getId()));
	}

	@Test
	void createWithoutAssigneeAndDueDate() {
		TodoResponse response = todoService.create(familyId, mom.getId(), new TodoCreateRequest("청소", null, null));

		assertThat(response.assigneeId()).isNull();
		assertThat(response.dueDate()).isNull();
	}

	@Test
	void createRejectsAssigneeOutsideFamily() {
		assertThatThrownBy(() -> todoService.create(familyId, mom.getId(),
				new TodoCreateRequest("장보기", stranger.getId(), null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_MEMBER_NOT_FOUND);
	}

	@Test
	void createRejectsUserOutsideFamily() {
		assertThatThrownBy(() -> todoService.create(familyId, stranger.getId(),
				new TodoCreateRequest("장보기", null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getTodosFiltersByDone() {
		Long open = create("장보기").id();
		Long finished = create("청소").id();
		todoService.update(finished, mom.getId(), done(true));

		assertThat(todoService.getTodos(familyId, dad.getId(), false)).extracting(TodoResponse::id)
				.containsExactly(open);
		assertThat(todoService.getTodos(familyId, dad.getId(), true)).extracting(TodoResponse::id)
				.containsExactly(finished);
		assertThat(todoService.getTodos(familyId, dad.getId(), null)).hasSize(2);
	}

	@Test
	void getTodosRejectsUserOutsideFamily() {
		assertThatThrownBy(() -> todoService.getTodos(familyId, stranger.getId(), null))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void updateChecksDoneAndKeepsOtherFields() {
		Long id = todoService.create(familyId, mom.getId(), new TodoCreateRequest("장보기", dad.getId(), OCT_2)).id();

		TodoResponse response = todoService.update(id, dad.getId(), done(true));

		assertThat(response).isEqualTo(new TodoResponse(id, "장보기", dad.getId(), OCT_2, true, mom.getId()));
	}

	@Test
	void updateChangesTitleAssigneeAndDueDate() {
		Long id = create("장보기").id();

		TodoResponse response = todoService.update(id, mom.getId(), updateRequest("마트",
				dad.getId(), OCT_2, false));

		assertThat(response).isEqualTo(new TodoResponse(id, "마트", dad.getId(), OCT_2, false, mom.getId()));
	}

	@Test
	void updateClearsAssigneeAndDueDateWithEmpty() {
		Long id = todoService.create(familyId, mom.getId(), new TodoCreateRequest("장보기", dad.getId(), OCT_2)).id();

		TodoResponse response = todoService.update(id, mom.getId(),
				clearAssigneeAndDueDate());

		assertThat(response.assigneeId()).isNull();
		assertThat(response.dueDate()).isNull();
	}

	@Test
	void updateRejectsAssigneeOutsideFamily() {
		Long id = create("장보기").id();

		assertThatThrownBy(() -> todoService.update(id, mom.getId(),
				assignee(stranger.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_MEMBER_NOT_FOUND);
	}

	@Test
	void updateRejectsUserOutsideFamily() {
		Long id = create("장보기").id();

		assertThatThrownBy(() -> todoService.update(id, stranger.getId(),
				done(true)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void updateUnknownTodoIsNotFound() {
		assertThatThrownBy(() -> todoService.update(999_999L, mom.getId(),
				done(true)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.TODO_NOT_FOUND);
	}

	@Test
	void deleteRemovesTodo() {
		Long id = create("장보기").id();

		todoService.delete(id, dad.getId());

		assertThat(todoRepository.findById(id)).isEmpty();
	}

	@Test
	void deleteRejectsUserOutsideFamily() {
		Long id = create("장보기").id();

		assertThatThrownBy(() -> todoService.delete(id, stranger.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
		assertThat(todoRepository.findById(id)).isPresent();
	}

	@Test
	void deleteUnknownTodoIsNotFound() {
		assertThatThrownBy(() -> todoService.delete(999_999L, mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.TODO_NOT_FOUND);
	}

	@Test
	void todosAreDeletedWhenLastMemberLeavesFamily() {
		todoService.create(familyId, mom.getId(), new TodoCreateRequest("장보기", dad.getId(), null));
		familyService.removeMember(familyId, dad.getId(), dad.getId());

		familyService.removeMember(familyId, mom.getId(), mom.getId());
		todoRepository.flush();

		assertThat(todoRepository.count()).isZero();
	}

	private TodoResponse create(String title) {
		return todoService.create(familyId, mom.getId(), new TodoCreateRequest(title, null, null));
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
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
