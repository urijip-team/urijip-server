package com.urijip.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @Async 메서드는 Spring Boot가 만드는 applicationTaskExecutor에서 돈다. 푸시 발송처럼 응답을 늦추면 안 되는 일에 쓴다.
@Configuration
@EnableAsync
public class AsyncConfig {
}
