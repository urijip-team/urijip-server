package com.urijip.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.global.security.UserStatusChecker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UrijipServerApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
	}

	// 기본 로그인 계정(생성된 비밀번호를 로그에 찍는다)을 만들지 않는다.
	@Test
	void noDefaultUserDetailsService() {
		assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
	}

	// 인증 필터는 이 빈이 있을 때만 정지·탈퇴 회원을 막는다(@WebMvcTest에는 없다).
	@Test
	void userStatusCheckerIsAvailableToTheFilter() {
		assertThat(context.getBeansOfType(UserStatusChecker.class)).hasSize(1);
	}

}
