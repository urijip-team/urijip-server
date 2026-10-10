package com.urijip.server.member.service;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.member.dto.request.LoginRequest;
import com.urijip.server.member.dto.request.SignupRequest;
import com.urijip.server.member.dto.response.LoginResponse;
import com.urijip.server.member.dto.response.SignupResponse;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
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
	private final JwtProvider jwtProvider;

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

	@Transactional
	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.LOGIN_FAILED);
		}
		validateStatus(user);

		String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
		String refreshToken = jwtProvider.createRefreshToken(user.getId());

		return LoginResponse.of(accessToken, refreshToken, user, false);
	}

	private void validateStatus(User user) {
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			throw new BusinessException(ErrorCode.LOGIN_FAILED);
		}
		if (user.getStatus() == UserStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.USER_SUSPENDED);
		}
	}

}
