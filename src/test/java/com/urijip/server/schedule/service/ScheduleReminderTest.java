package com.urijip.server.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.urijip.server.global.time.SeoulTime;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

class ScheduleReminderTest {

	private final ScheduleReminderService service = mock(ScheduleReminderService.class);

	private final ScheduleReminder reminder = new ScheduleReminder(service);

	@Test
	void remindTomorrowPassesTodayInSeoul() {
		reminder.remindTomorrow();

		verify(service).remindTomorrow(LocalDate.now(SeoulTime.ZONE));
	}

	@Test
	void remindTodayPassesTodayInSeoul() {
		reminder.remindToday();

		verify(service).remindToday(LocalDate.now(SeoulTime.ZONE));
	}

	@Test
	void failureIsLoggedNotThrown() {
		willThrow(new IllegalStateException("db down")).given(service).remindToday(any());

		assertThatCode(reminder::remindToday).doesNotThrowAnyException();
	}

	@Test
	void runsAtEightAndTwentyInSeoul() throws NoSuchMethodException {
		Scheduled tomorrow = ScheduleReminder.class.getMethod("remindTomorrow").getAnnotation(Scheduled.class);
		Scheduled today = ScheduleReminder.class.getMethod("remindToday").getAnnotation(Scheduled.class);
		LocalDateTime morning = LocalDateTime.of(2030, 5, 5, 7, 0);

		assertThat(tomorrow.zone()).isEqualTo("Asia/Seoul");
		assertThat(today.zone()).isEqualTo("Asia/Seoul");
		assertThat(CronExpression.parse(tomorrow.cron()).next(morning)).isEqualTo(morning.withHour(20));
		assertThat(CronExpression.parse(today.cron()).next(morning)).isEqualTo(morning.withHour(8));
	}

}
