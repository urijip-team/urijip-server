package com.urijip.server.schedule.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.push.PushMessage;
import com.urijip.server.global.push.PushType;
import com.urijip.server.member.entity.User;
import com.urijip.server.notification.service.NotificationService;
import com.urijip.server.schedule.entity.Schedule;
import com.urijip.server.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 일정 알림(CAL-04). 그날 시작하는 일정을 참여자에게, 참여자가 없으면 가족 모두에게 푸시한다.
@Service
@RequiredArgsConstructor
public class ScheduleReminderService {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);

	private final ScheduleRepository scheduleRepository;
	private final FamilyService familyService;
	private final NotificationService notificationService;

	// 당일 아침 알림. today에 시작하는 일정.
	@Transactional(readOnly = true)
	public void remindToday(LocalDate today) {
		remind(today, "오늘 일정");
	}

	// 전날 저녁 알림. today 다음 날에 시작하는 일정.
	@Transactional(readOnly = true)
	public void remindTomorrow(LocalDate today) {
		remind(today.plusDays(1), "내일 일정");
	}

	private void remind(LocalDate date, String title) {
		List<Schedule> schedules = scheduleRepository.findStartingBetween(date.atStartOfDay(),
				date.plusDays(1).atStartOfDay());
		Map<Long, Set<Long>> membersByFamily = new HashMap<>();
		for (Schedule schedule : schedules) {
			// 정지된 가족에는 일정 알림을 보내지 않는다.
			if (schedule.getFamily().isSuspended()) {
				continue;
			}
			Set<Long> members = membersByFamily.computeIfAbsent(schedule.getFamily().getId(), this::memberIds);
			notificationService.send(receivers(schedule, members), new PushMessage(PushType.SCHEDULE, title,
					body(schedule), Map.of("scheduleId", schedule.getId().toString(), "date", date.toString())));
		}
	}

	// 가족을 나간 참여자는 뺀다.
	private Set<Long> receivers(Schedule schedule, Set<Long> members) {
		if (schedule.getParticipantIds().isEmpty()) {
			return members;
		}
		return schedule.getParticipantIds().stream().filter(members::contains).collect(Collectors.toSet());
	}

	private String body(Schedule schedule) {
		String when = schedule.isAllDay() ? "하루 종일" : schedule.getStartAt().format(TIME);
		return schedule.getTitle() + " (" + when + ")";
	}

	private Set<Long> memberIds(Long familyId) {
		return familyService.getMemberUsers(familyId).stream().map(User::getId).collect(Collectors.toSet());
	}

}
