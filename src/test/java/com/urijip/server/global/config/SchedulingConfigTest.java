package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

class SchedulingConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
			.withUserConfiguration(SchedulingConfig.class)
			.withBean(Ticker.class);

	@Test
	void scheduledMethodRuns() {
		runner.run(context -> assertThat(context.getBean(Ticker.class).ticked.await(3, TimeUnit.SECONDS)).isTrue());
	}

	static class Ticker {

		final CountDownLatch ticked = new CountDownLatch(1);

		@Scheduled(fixedDelay = 100)
		void tick() {
			ticked.countDown();
		}

	}

}
