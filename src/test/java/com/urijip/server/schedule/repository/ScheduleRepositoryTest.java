package com.urijip.server.schedule.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.schedule.entity.Schedule;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class ScheduleRepositoryTest {

	private static final LocalDateTime OCT_1 = LocalDateTime.of(2026, 10, 1, 0, 0);
	private static final LocalDateTime NOV_1 = LocalDateTime.of(2026, 11, 1, 0, 0);

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	private User mom;

	private Family family;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com");
		family = saveFamily("A3K9P2", mom);
	}

	@Test
	void savedScheduleKeepsParticipants() {
		Schedule saved = save(family, "가족 여행", at(10, 2, 9), at(10, 4, 18), List.of(mom.getId(), 99L));
		entityManager.clear();

		Schedule found = scheduleRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getParticipantIds()).containsExactlyInAnyOrder(mom.getId(), 99L);
		assertThat(found.getCreatedBy().getId()).isEqualTo(mom.getId());
	}

	@Test
	void findOverlappingReturnsSchedulesInMonthOrderedByStart() {
		Schedule later = save(family, "병원", at(10, 20, 10), at(10, 20, 11), List.of());
		Schedule earlier = save(family, "장보기", at(10, 2, 10), at(10, 2, 11), List.of());

		assertThat(scheduleRepository.findOverlapping(family.getId(), OCT_1, NOV_1))
				.extracting(Schedule::getId)
				.containsExactly(earlier.getId(), later.getId());
	}

	@Test
	void findOverlappingIncludesSchedulesCrossingMonthBoundary() {
		Schedule fromSeptember = save(family, "추석", at(9, 30, 0), at(10, 1, 0), List.of());
		Schedule toNovember = save(family, "출장", at(10, 31, 9), at(11, 2, 18), List.of());

		assertThat(scheduleRepository.findOverlapping(family.getId(), OCT_1, NOV_1))
				.extracting(Schedule::getId)
				.containsExactly(fromSeptember.getId(), toNovember.getId());
	}

	@Test
	void findOverlappingExcludesSchedulesOutsideMonth() {
		save(family, "9월", at(9, 29, 10), at(9, 30, 23), List.of());
		save(family, "11월", LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 1, 1, 0), List.of());

		assertThat(scheduleRepository.findOverlapping(family.getId(), OCT_1, NOV_1)).isEmpty();
	}

	@Test
	void findOverlappingExcludesOtherFamilies() {
		Family other = saveFamily("B4L8Q3", saveUser("dad@example.com"));
		save(other, "다른 집 일정", at(10, 2, 10), at(10, 2, 11), List.of());

		assertThat(scheduleRepository.findOverlapping(family.getId(), OCT_1, NOV_1)).isEmpty();
	}

	@Test
	void deleteByFamilyIdDeletesSchedulesAndParticipantsOfThatFamilyOnly() {
		save(family, "장보기", at(10, 2, 10), at(10, 2, 11), List.of(mom.getId()));
		Family other = saveFamily("B4L8Q3", saveUser("dad@example.com"));
		Schedule kept = save(other, "다른 집 일정", at(10, 2, 10), at(10, 2, 11), List.of());

		long deleted = scheduleRepository.deleteByFamilyId(family.getId());
		entityManager.flush();

		assertThat(deleted).isEqualTo(1);
		assertThat(scheduleRepository.findAll()).extracting(Schedule::getId).containsExactly(kept.getId());
		assertThat(entityManager.createNativeQuery("select count(*) from schedule_participants").getSingleResult())
				.isEqualTo(0L);
	}

	private Schedule save(Family family, String title, LocalDateTime startAt, LocalDateTime endAt,
			List<Long> participantIds) {
		return scheduleRepository.saveAndFlush(Schedule.builder()
				.family(family)
				.title(title)
				.startAt(startAt)
				.endAt(endAt)
				.createdBy(mom)
				.participantIds(participantIds)
				.build());
	}

	private LocalDateTime at(int month, int day, int hour) {
		return LocalDateTime.of(2026, month, day, hour, 0);
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
