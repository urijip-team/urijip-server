package com.urijip.server.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.push.PushType;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notification.service.PushRequestedEvent;
import com.urijip.server.schedule.dto.request.ScheduleCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RecordApplicationEvents
class ScheduleReminderServiceTest {

	private static final LocalDate DAY = LocalDate.of(2030, 5, 5);

	@Autowired
	private ScheduleReminderService scheduleReminderService;

	@Autowired
	private ScheduleService scheduleService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ApplicationEvents events;

	private User mom;

	private User dad;

	private User kid;

	private Long familyId;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		dad = saveUser("dad@example.com", "김아빠");
		kid = saveUser("kid@example.com", "김첫째");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("우리집"));
		familyService.join(dad.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyService.join(kid.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyId = family.id();
	}

	@Test
	void remindTodayPushesParticipantsWithTime() {
		Long scheduleId = create("병원", DAY.atTime(15, 0), false, List.of(kid.getId()));

		scheduleReminderService.remindToday(DAY);

		assertThat(events.stream(PushRequestedEvent.class)).singleElement().satisfies(event -> {
			assertThat(event.userIds()).isEqualTo(Set.of(kid.getId()));
			assertThat(event.message().type()).isEqualTo(PushType.SCHEDULE);
			assertThat(event.message().title()).isEqualTo("오늘 일정");
			assertThat(event.message().body()).isEqualTo("병원 (오후 3:00)");
			assertThat(event.message().data()).isEqualTo(Map.of("scheduleId", scheduleId.toString(),
					"date", "2030-05-05"));
		});
	}

	@Test
	void remindTomorrowPushesSchedulesOfNextDay() {
		Long scheduleId = create("병원", DAY.atTime(9, 30), false, List.of(kid.getId()));
		create("오늘 일정", DAY.minusDays(1).atTime(9, 30), false, List.of(kid.getId()));

		scheduleReminderService.remindTomorrow(DAY.minusDays(1));

		assertThat(events.stream(PushRequestedEvent.class)).singleElement().satisfies(event -> {
			assertThat(event.userIds()).isEqualTo(Set.of(kid.getId()));
			assertThat(event.message().title()).isEqualTo("내일 일정");
			assertThat(event.message().body()).isEqualTo("병원 (오전 9:30)");
			assertThat(event.message().data()).isEqualTo(Map.of("scheduleId", scheduleId.toString(),
					"date", "2030-05-05"));
		});
	}

	@Test
	void scheduleWithoutParticipantsGoesToWholeFamily() {
		create("가족 외식", DAY.atTime(18, 30), false, List.of());

		scheduleReminderService.remindToday(DAY);

		assertThat(events.stream(PushRequestedEvent.class)).singleElement()
				.satisfies(event -> assertThat(event.userIds()).isEqualTo(Set.of(mom.getId(), dad.getId(), kid.getId())));
	}

	@Test
	void allDayScheduleSaysAllDay() {
		create("어린이날", DAY.atStartOfDay(), true, List.of());

		scheduleReminderService.remindToday(DAY);

		assertThat(events.stream(PushRequestedEvent.class)).singleElement()
				.satisfies(event -> assertThat(event.message().body()).isEqualTo("어린이날 (하루 종일)"));
	}

	@Test
	void participantsWhoLeftTheFamilyAreSkipped() {
		create("병원", DAY.atTime(9, 0), false, List.of(kid.getId(), dad.getId()));
		familyService.leaveFamily(kid.getId());

		scheduleReminderService.remindToday(DAY);

		assertThat(events.stream(PushRequestedEvent.class)).singleElement()
				.satisfies(event -> assertThat(event.userIds()).isEqualTo(Set.of(dad.getId())));
	}

	@Test
	void schedulesOfOtherDaysAreNotReminded() {
		create("전날", DAY.minusDays(1).atTime(23, 0), false, List.of());
		create("다음 날", DAY.plusDays(1).atTime(0, 0), false, List.of());

		scheduleReminderService.remindToday(DAY);

		assertThat(events.stream(PushRequestedEvent.class)).isEmpty();
	}

	private Long create(String title, LocalDateTime startAt, boolean allDay, List<Long> participantIds) {
		return scheduleService.create(familyId, mom.getId(), new ScheduleCreateRequest(title, null,
				SeoulTime.toOffset(startAt), SeoulTime.toOffset(startAt.plusHours(1)), allDay, participantIds)).id();
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder().email(email).password("encoded-password").name(name).build());
	}

}
