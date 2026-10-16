package com.urijip.server.schedule.service;

import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.urijip.server.family.service.FamilyDeletedEvent;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.service.UserService;
import com.urijip.server.schedule.dto.request.ScheduleCreateRequest;
import com.urijip.server.schedule.dto.request.ScheduleUpdateRequest;
import com.urijip.server.schedule.dto.response.BirthdayResponse;
import com.urijip.server.schedule.dto.response.MonthlyScheduleResponse;
import com.urijip.server.schedule.dto.response.ScheduleResponse;
import com.urijip.server.schedule.entity.Schedule;
import com.urijip.server.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

	private final ScheduleRepository scheduleRepository;
	private final FamilyService familyService;
	private final UserService userService;

	@Transactional
	public ScheduleResponse create(Long familyId, Long userId, ScheduleCreateRequest request) {
		familyService.validateMember(familyId, userId);
		List<Long> participantIds = Objects.requireNonNullElse(request.participantIds(), List.of());
		familyService.validateMembers(familyId, participantIds);

		Schedule schedule = scheduleRepository.save(Schedule.builder()
				.family(familyService.getFamily(familyId))
				.title(request.title())
				.memo(blankToNull(request.memo()))
				.startAt(SeoulTime.toLocal(request.startAt()))
				.endAt(SeoulTime.toLocal(request.endAt()))
				.allDay(Boolean.TRUE.equals(request.allDay()))
				.createdBy(userService.getUser(userId))
				.participantIds(participantIds)
				.build());

		return ScheduleResponse.from(schedule);
	}

	@Transactional(readOnly = true)
	public MonthlyScheduleResponse getMonthly(Long familyId, Long userId, YearMonth month) {
		familyService.validateMember(familyId, userId);
		LocalDateTime from = month.atDay(1).atStartOfDay();
		LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();

		List<ScheduleResponse> schedules = scheduleRepository.findOverlapping(familyId, from, to).stream()
				.map(ScheduleResponse::from)
				.toList();
		return new MonthlyScheduleResponse(schedules, birthdaysIn(familyId, month));
	}

	@Transactional
	public ScheduleResponse update(Long scheduleId, Long userId, ScheduleUpdateRequest request) {
		Schedule schedule = getAccessibleSchedule(scheduleId, userId);

		if (request.title() != null) {
			schedule.changeTitle(request.title());
		}
		if (request.memo() != null) {
			schedule.changeMemo(blankToNull(request.memo()));
		}
		LocalDateTime startAt = Objects.requireNonNullElse(SeoulTime.toLocal(request.startAt()), schedule.getStartAt());
		LocalDateTime endAt = Objects.requireNonNullElse(SeoulTime.toLocal(request.endAt()), schedule.getEndAt());
		if (endAt.isBefore(startAt)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		schedule.changePeriod(startAt, endAt, Objects.requireNonNullElse(request.allDay(), schedule.isAllDay()));
		if (request.participantIds() != null) {
			familyService.validateMembers(schedule.getFamily().getId(), request.participantIds());
			schedule.changeParticipants(request.participantIds());
		}

		return ScheduleResponse.from(schedule);
	}

	@Transactional
	public void delete(Long scheduleId, Long userId) {
		scheduleRepository.delete(getAccessibleSchedule(scheduleId, userId));
	}

	@EventListener
	public void onFamilyDeleted(FamilyDeletedEvent event) {
		scheduleRepository.deleteByFamilyId(event.familyId());
	}

	private Schedule getAccessibleSchedule(Long scheduleId, Long userId) {
		Schedule schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
		familyService.validateMember(schedule.getFamily().getId(), userId);
		return schedule;
	}

	// 생일은 해마다 반복되므로 그 달에 해당하는 날짜로 바꿔 내보낸다. 2월 29일생은 평년에 2월 28일이 된다.
	private List<BirthdayResponse> birthdaysIn(Long familyId, YearMonth month) {
		return familyService.getMemberUsers(familyId).stream()
				.filter(user -> user.getBirthday() != null && user.getBirthday().getMonth() == month.getMonth())
				.map(user -> new BirthdayResponse(user.getId(), user.getName(),
						MonthDay.from(user.getBirthday()).atYear(month.getYear())))
				.sorted(Comparator.comparing(BirthdayResponse::date).thenComparing(BirthdayResponse::userId))
				.toList();
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

}
