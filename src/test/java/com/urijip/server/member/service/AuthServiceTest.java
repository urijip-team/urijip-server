package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.dto.request.SignupRequest;
import com.urijip.server.member.dto.response.SignupResponse;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void signupSavesUserWithEncodedPassword() {
		SignupResponse response = authService.signup(
				new SignupRequest("mom@example.com", "password123", "김엄마"));

		User saved = userRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getEmail()).isEqualTo("mom@example.com");
		assertThat(saved.getName()).isEqualTo("김엄마");
		assertThat(saved.getPassword()).isNotEqualTo("password123");
		assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
	}

	@Test
	void signupReturnsSavedUserWithoutPassword() {
		SignupResponse response = authService.signup(
				new SignupRequest("mom@example.com", "password123", "김엄마"));

		assertThat(response.id()).isNotNull();
		assertThat(response.email()).isEqualTo("mom@example.com");
		assertThat(response.name()).isEqualTo("김엄마");
	}

	@Test
	void signupRejectsDuplicatedEmail() {
		SignupRequest request = new SignupRequest("mom@example.com", "password123", "김엄마");
		authService.signup(request);

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EMAIL_DUPLICATED);
		assertThat(userRepository.count()).isEqualTo(1);
	}

}
