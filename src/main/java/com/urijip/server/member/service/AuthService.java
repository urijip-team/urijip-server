package com.urijip.server.member.service;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.dto.request.SignupRequest;
import com.urijip.server.member.dto.response.SignupResponse;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
		}

		User user = User.builder()
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.name(request.name())
				.build();

		return SignupResponse.from(userRepository.save(user));
	}

}
