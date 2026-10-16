package com.urijip.server.schedule.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.schedule.entity.Todo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class TodoRepositoryTest {

	@Autowired
	private TodoRepository todoRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private Family family;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com");
		family = saveFamily("A3K9P2", mom);
	}

	@Test
	void newTodoIsNotDone() {
		Todo saved = save(family, "장보기", LocalDate.of(2026, 10, 2));

		assertThat(saved.isDone()).isFalse();
		assertThat(saved.getAssignee()).isNull();
	}

	@Test
	void findByFamilyOrdersByDueDateWithoutDueDateLast() {
		Todo noDue = save(family, "청소", null);
		Todo later = save(family, "병원 예약", LocalDate.of(2026, 10, 20));
		Todo earlier = save(family, "장보기", LocalDate.of(2026, 10, 2));

		assertThat(todoRepository.findByFamily(family.getId(), null))
				.extracting(Todo::getId)
				.containsExactly(earlier.getId(), later.getId(), noDue.getId());
	}

	@Test
	void findByFamilyFiltersByDone() {
		Todo open = save(family, "장보기", null);
		Todo finished = save(family, "청소", null);
		finished.changeDone(true);
		todoRepository.flush();

		assertThat(todoRepository.findByFamily(family.getId(), false)).extracting(Todo::getId)
				.containsExactly(open.getId());
		assertThat(todoRepository.findByFamily(family.getId(), true)).extracting(Todo::getId)
				.containsExactly(finished.getId());
	}

	@Test
	void findByFamilyExcludesOtherFamilies() {
		save(saveFamily("B4L8Q3", saveUser("dad@example.com")), "다른 집 할 일", null);

		assertThat(todoRepository.findByFamily(family.getId(), null)).isEmpty();
	}

	@Test
	void deleteByFamilyIdDeletesOnlyThatFamily() {
		save(family, "장보기", null);
		Todo kept = save(saveFamily("B4L8Q3", saveUser("dad@example.com")), "다른 집 할 일", null);

		long deleted = todoRepository.deleteByFamilyId(family.getId());

		assertThat(deleted).isEqualTo(1);
		assertThat(todoRepository.findAll()).extracting(Todo::getId).containsExactly(kept.getId());
	}

	private Todo save(Family family, String title, LocalDate dueDate) {
		return todoRepository.saveAndFlush(Todo.builder()
				.family(family)
				.title(title)
				.dueDate(dueDate)
				.createdBy(mom)
				.build());
	}

	private Family saveFamily(String inviteCode, User owner) {
		return familyRepository.saveAndFlush(Family.builder()
				.name("김씨네")
				.inviteCode(inviteCode)
				.owner(owner)
				.build());
	}

	private User saveUser(String email) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name("김엄마")
				.build());
	}

}
