package com.urijip.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Async;

class AsyncConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
			.withUserConfiguration(AsyncConfig.class)
			.withBean(Worker.class);

	@Test
	void asyncMethodRunsOnAnotherThread() {
		runner.run(context -> {
			String caller = Thread.currentThread().getName();

			String worker = context.getBean(Worker.class).threadName().join();

			assertThat(worker).isNotEqualTo(caller);
		});
	}

	static class Worker {

		@Async
		CompletableFuture<String> threadName() {
			return CompletableFuture.completedFuture(Thread.currentThread().getName());
		}

	}

}
