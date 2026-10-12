package com.urijip.server.member.service;

import java.time.LocalDateTime;

import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProperties;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.member.dto.request.LoginRequest;
import com.urijip.server.member.dto.request.LogoutRequest;
import com.urijip.server.member.dto.request.ReissueRequest;
import com.urijip.server.member.dto.request.SignupRequest;
import com.urijip.server.member.dto.response.AdminLoginResponse;
import com.urijip.server.member.dto.response.LoginResponse;
import com.urijip.server.member.dto.response.ReissueResponse;
import com.urijip.server.member.dto.response.SignupResponse;
import com.urijip.server.member.entity.RefreshToken;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.repository.RefreshTokenRepository;
import com.urijip.server.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final JwtProperties jwtProperties;
	private final FamilyService familyService;

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
		User user = findByCredentials(request);
		validateStatus(user, ErrorCode.LOGIN_FAILED);

		String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
		String refreshToken = issueRefreshToken(user);

		return LoginResponse.of(accessToken, refreshToken, user, familyService.hasFamily(user.getId()));
	}

	@Transactional
	public AdminLoginResponse adminLogin(LoginRequest request) {
		User admin = findByCredentials(request);
		if (admin.getRole() != Role.ADMIN) {
			throw new BusinessException(ErrorCode.LOGIN_FAILED);
		}
		validateStatus(admin, ErrorCode.LOGIN_FAILED);

		String accessToken = jwtProvider.createAccessToken(admin.getId(), admin.getRole().name());
		String refreshToken = issueRefreshToken(admin);

		return AdminLoginResponse.of(accessToken, refreshToken, admin);
	}

	@Transactional
	public ReissueResponse reissue(ReissueRequest request) {
		jwtProvider.parseRefreshToken(request.refreshToken());
		RefreshToken saved = refreshTokenRepository.findByToken(request.refreshToken())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
		User user = saved.getUser();
		validateStatus(user, ErrorCode.INVALID_TOKEN);

		refreshTokenRepository.delete(saved);
		String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
		String refreshToken = issueRefreshToken(user);

		return new ReissueResponse(accessToken, refreshToken);
	}

	@Transactional
	public void logout(Long userId, LogoutRequest request) {
		refreshTokenRepository.deleteByTokenAndUserId(request.refreshToken(), userId);
	}

	@Transactional
	public void withdraw(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		user.withdraw();
	}

	private User findByCredentials(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BusinessException(ErrorCode.LOGIN_FAILED);
		}
		return user;
	}

	private String issueRefreshToken(User user) {
		String refreshToken = jwtProvider.createRefreshToken(user.getId());
		refreshTokenRepository.save(new RefreshToken(user, refreshToken,
				LocalDateTime.now().plus(jwtProperties.refreshExpiration())));
		return refreshToken;
	}

	private void validateStatus(User user, ErrorCode withdrawnError) {
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			throw new BusinessException(withdrawnError);
		}
		if (user.getStatus() == UserStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.USER_SUSPENDED);
		}
	}

}
