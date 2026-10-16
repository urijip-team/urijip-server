package com.urijip.server.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.schedule.dto.request.ScheduleCreateRequest;
import com.urijip.server.schedule.dto.request.ScheduleUpdateRequest;
import com.urijip.server.schedule.dto.response.BirthdayResponse;
import com.urijip.server.schedule.dto.response.MonthlyScheduleResponse;
import com.urijip.server.schedule.dto.response.ScheduleResponse;
import com.urijip.server.schedule.entity.Schedule;
import com.urijip.server.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ScheduleServiceTest {

	private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

	@Autowired
	private ScheduleService scheduleService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private ScheduleRepository scheduleRepository;

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
	void createReturnsScheduleInSeoulTime() {
		ScheduleResponse response = scheduleService.create(familyId, mom.getId(), new ScheduleCreateRequest("가족 여행",
				"속초", kst(10, 2, 9), kst(10, 4, 18), false, List.of(dad.getId(), mom.getId())));

		assertThat(response.id()).isNotNull();
		assertThat(response.title()).isEqualTo("가족 여행");
		assertThat(response.memo()).isEqualTo("속초");
		assertThat(response.startAt()).isEqualTo(kst(10, 2, 9));
		assertThat(response.startAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
		assertThat(response.endAt()).isEqualTo(kst(10, 4, 18));
		assertThat(response.allDay()).isFalse();
		assertThat(response.participantIds()).containsExactly(mom.getId(), dad.getId());
		assertThat(response.createdBy()).isEqualTo(mom.getId());
	}

	@Test
	void createStoresSeoulLocalTimeEvenIfRequestUsesOtherOffset() {
		OffsetDateTime utc = OffsetDateTime.of(2026, 10, 2, 0, 0, 0, 0, ZoneOffset.UTC);

		ScheduleResponse response = scheduleService.create(familyId, mom.getId(),
				new ScheduleCreateRequest("회의", null, utc, utc.plusHours(1), false, null));

		Schedule saved = scheduleRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getStartAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 9, 0));
		assertThat(response.startAt()).isEqualTo(kst(10, 2, 9));
	}

	@Test
	void createWithoutParticipantsAndBlankMemo() {
		ScheduleResponse response = scheduleService.create(familyId, mom.getId(),
				new ScheduleCreateRequest("장보기", " ", kst(10, 2, 9), kst(10, 2, 10), true, null));

		assertThat(response.memo()).isNull();
		assertThat(response.participantIds()).isEmpty();
		assertThat(response.allDay()).isTrue();
	}

	@Test
	void createWithoutAllDayIsNotAllDay() {
		assertThat(scheduleService.create(familyId, mom.getId(), request("장보기")).allDay()).isFalse();
	}

	@Test
	void createRejectsUserOutsideFamily() {
		assertThatThrownBy(() -> scheduleService.create(familyId, stranger.getId(), request("장보기")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void createRejectsParticipantOutsideFamily() {
		assertThatThrownBy(() -> scheduleService.create(familyId, mom.getId(), new ScheduleCreateRequest("장보기",
				null, kst(10, 2, 9), kst(10, 2, 10), false, List.of(dad.getId(), stranger.getId()))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_MEMBER_NOT_FOUND);
	}

	@Test
	void createAcceptsDuplicatedParticipantIds() {
		ScheduleResponse response = scheduleService.create(familyId, mom.getId(), new ScheduleCreateRequest("장보기",
				null, kst(10, 2, 9), kst(10, 2, 10), false, List.of(dad.getId(), dad.getId())));

		assertThat(response.participantIds()).containsExactly(dad.getId());
	}

	@Test
	void getMonthlyReturnsSchedulesOfMonthInOrder() {
		Long later = scheduleService.create(familyId, mom.getId(), request("병원", kst(10, 20, 10))).id();
		Long earlier = scheduleService.create(familyId, dad.getId(), request("장보기", kst(10, 2, 10))).id();
		scheduleService.create(familyId, mom.getId(), request("다음 달", kst(11, 2, 10)));

		MonthlyScheduleResponse response = scheduleService.getMonthly(familyId, dad.getId(), OCTOBER);

		assertThat(response.schedules()).extracting(ScheduleResponse::id).containsExactly(earlier, later);
	}

	@Test
	void getMonthlyIncludesBirthdaysOfFamilyInThatMonth() {
		setBirthday(mom, LocalDate.of(1980, 10, 25));
		setBirthday(dad, LocalDate.of(1978, 10, 3));
		setBirthday(stranger, LocalDate.of(1990, 10, 1));

		MonthlyScheduleResponse response = scheduleService.getMonthly(familyId, mom.getId(), OCTOBER);

		assertThat(response.birthdays()).containsExactly(
				new BirthdayResponse(dad.getId(), "김아빠", LocalDate.of(2026, 10, 3)),
				new BirthdayResponse(mom.getId(), "김엄마", LocalDate.of(2026, 10, 25)));
	}

	@Test
	void getMonthlyExcludesBirthdaysOfOtherMonthsAndUnknownBirthdays() {
		setBirthday(mom, LocalDate.of(1980, 5, 5));

		MonthlyScheduleResponse response = scheduleService.getMonthly(familyId, mom.getId(), OCTOBER);

		assertThat(response.birthdays()).isEmpty();
		assertThat(response.schedules()).isEmpty();
	}

	@Test
	void leapDayBirthdayIsShownOnFebruary28InCommonYear() {
		setBirthday(mom, LocalDate.of(2000, 2, 29));

		assertThat(scheduleService.getMonthly(familyId, mom.getId(), YearMonth.of(2027, 2)).birthdays())
				.extracting(BirthdayResponse::date)
				.containsExactly(LocalDate.of(2027, 2, 28));
		assertThat(scheduleService.getMonthly(familyId, mom.getId(), YearMonth.of(2028, 2)).birthdays())
				.extracting(BirthdayResponse::date)
				.containsExactly(LocalDate.of(2028, 2, 29));
	}

	@Test
	void getMonthlyRejectsUserOutsideFamily() {
		assertThatThrownBy(() -> scheduleService.getMonthly(familyId, stranger.getId(), OCTOBER))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void updateChangesOnlySentFields() {
		Long id = scheduleService.create(familyId, mom.getId(), new ScheduleCreateRequest("장보기", "우유",
				kst(10, 2, 9), kst(10, 2, 10), false, List.of(mom.getId()))).id();

		ScheduleResponse response = scheduleService.update(id, dad.getId(),
				new ScheduleUpdateRequest("마트", null, null, kst(10, 2, 11), null, null));

		assertThat(response.title()).isEqualTo("마트");
		assertThat(response.memo()).isEqualTo("우유");
		assertThat(response.startAt()).isEqualTo(kst(10, 2, 9));
		assertThat(response.endAt()).isEqualTo(kst(10, 2, 11));
		assertThat(response.participantIds()).containsExactly(mom.getId());
		assertThat(response.createdBy()).isEqualTo(mom.getId());
	}

	@Test
	void updateClearsMemoAndParticipants() {
		Long id = scheduleService.create(familyId, mom.getId(), new ScheduleCreateRequest("장보기", "우유",
				kst(10, 2, 9), kst(10, 2, 10), false, List.of(mom.getId()))).id();

		ScheduleResponse response = scheduleService.update(id, mom.getId(),
				new ScheduleUpdateRequest(null, "", null, null, true, List.of()));

		assertThat(response.memo()).isNull();
		assertThat(response.participantIds()).isEmpty();
		assertThat(response.allDay()).isTrue();
	}

	@Test
	void updateRejectsEndBeforeStart() {
		Long id = scheduleService.create(familyId, mom.getId(), request("장보기", kst(10, 2, 10))).id();

		assertThatThrownBy(() -> scheduleService.update(id, mom.getId(),
				new ScheduleUpdateRequest(null, null, kst(10, 3, 10), null, null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);
	}

	@Test
	void updateRejectsParticipantOutsideFamily() {
		Long id = scheduleService.create(familyId, mom.getId(), request("장보기")).id();

		assertThatThrownBy(() -> scheduleService.update(id, mom.getId(),
				new ScheduleUpdateRequest(null, null, null, null, null, List.of(stranger.getId()))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_MEMBER_NOT_FOUND);
	}

	@Test
	void updateRejectsUserOutsideFamily() {
		Long id = scheduleService.create(familyId, mom.getId(), request("장보기")).id();

		assertThatThrownBy(() -> scheduleService.update(id, stranger.getId(),
				new ScheduleUpdateRequest("마트", null, null, null, null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void updateUnknownScheduleIsNotFound() {
		assertThatThrownBy(() -> scheduleService.update(999_999L, mom.getId(),
				new ScheduleUpdateRequest("마트", null, null, null, null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
	}

	@Test
	void deleteRemovesSchedule() {
		Long id = scheduleService.create(familyId, mom.getId(), request("장보기")).id();

		scheduleService.delete(id, dad.getId());

		assertThat(scheduleRepository.findById(id)).isEmpty();
	}

	@Test
	void deleteRejectsUserOutsideFamily() {
		Long id = scheduleService.create(familyId, mom.getId(), request("장보기")).id();

		assertThatThrownBy(() -> scheduleService.delete(id, stranger.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
		assertThat(scheduleRepository.findById(id)).isPresent();
	}

	@Test
	void deleteUnknownScheduleIsNotFound() {
		assertThatThrownBy(() -> scheduleService.delete(999_999L, mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
	}

	@Test
	void schedulesAreDeletedWhenLastMemberLeavesFamily() {
		scheduleService.create(familyId, mom.getId(), request("장보기"));
		familyService.removeMember(familyId, dad.getId(), dad.getId());

		familyService.removeMember(familyId, mom.getId(), mom.getId());
		scheduleRepository.flush();

		assertThat(scheduleRepository.count()).isZero();
	}

	private ScheduleCreateRequest request(String title) {
		return request(title, kst(10, 2, 10));
	}

	private ScheduleCreateRequest request(String title, OffsetDateTime startAt) {
		return new ScheduleCreateRequest(title, null, startAt, startAt.plusHours(1), null, null);
	}

	private OffsetDateTime kst(int month, int day, int hour) {
		return OffsetDateTime.of(2026, month, day, hour, 0, 0, 0, ZoneOffset.ofHours(9));
	}

	private void setBirthday(User user, LocalDate birthday) {
		ReflectionTestUtils.setField(user, "birthday", birthday);
		userRepository.flush();
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
