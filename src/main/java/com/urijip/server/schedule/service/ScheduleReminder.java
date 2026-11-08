package com.urijip.server.schedule.service;

import java.time.LocalDate;

import com.urijip.server.global.time.SeoulTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 일정 알림을 서울 시간 매일 20시(내일 일정)와 8시(오늘 일정)에 보낸다.
 * 서버를 여러 대 띄우면 대마다 보내므로, 그때는 한 대에서만 돌게 바꿔야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleReminder {

	static final String TOMORROW_CRON = "0 0 20 * * *";

	static final String TODAY_CRON = "0 0 8 * * *";

	static final String ZONE = "Asia/Seoul";

	private final ScheduleReminderService scheduleReminderService;

	@Scheduled(cron = TOMORROW_CRON, zone = ZONE)
	public void remindTomorrow() {
		try {
			scheduleReminderService.remindTomorrow(LocalDate.now(SeoulTime.ZONE));
		} catch (RuntimeException e) {
			log.error("Tomorrow schedule reminder failed", e);
		}
	}

	@Scheduled(cron = TODAY_CRON, zone = ZONE)
	public void remindToday() {
		try {
			scheduleReminderService.remindToday(LocalDate.now(SeoulTime.ZONE));
		} catch (RuntimeException e) {
			log.error("Today schedule reminder failed", e);
		}
	}

}
