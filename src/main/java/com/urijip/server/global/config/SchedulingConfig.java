package com.urijip.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled 메서드를 정해진 시각에 돌린다. 예: 일정 알림(ScheduleReminder).
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
