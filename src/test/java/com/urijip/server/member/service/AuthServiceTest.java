package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.AccessTokenClaims;
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
import com.urijip.server.member.entity.AdminLevel;
import com.urijip.server.member.entity.RefreshToken;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.repository.RefreshTokenRepository;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private JwtProperties jwtProperties;

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

	@Test
	void loginReturnsAccessTokenOfUser() {
		SignupResponse mom = signupMom();

		LoginResponse response = authService.login(new LoginRequest("mom@example.com", "password123"));

		AccessTokenClaims claims = jwtProvider.parseAccessToken(response.accessToken());
		assertThat(claims.userId()).isEqualTo(mom.id());
		assertThat(claims.role()).isEqualTo("USER");
	}

	@Test
	void loginReturnsRefreshTokenOfUser() {
		SignupResponse mom = signupMom();

		LoginResponse response = authService.login(new LoginRequest("mom@example.com", "password123"));

		assertThat(jwtProvider.parseRefreshToken(response.refreshToken())).isEqualTo(mom.id());
	}

	@Test
	void loginReturnsUserInfoWithoutFamily() {
		SignupResponse mom = signupMom();

		LoginResponse response = authService.login(new LoginRequest("mom@example.com", "password123"));

		assertThat(response.user().id()).isEqualTo(mom.id());
		assertThat(response.user().name()).isEqualTo("김엄마");
		assertThat(response.user().hasFamily()).isFalse();
	}

	@Test
	void loginReturnsHasFamilyForUserInFamily() {
		SignupResponse mom = signupMom();
		familyService.create(mom.id(), new FamilyCreateRequest("김씨네"));

		LoginResponse response = authService.login(new LoginRequest("mom@example.com", "password123"));

		assertThat(response.user().hasFamily()).isTrue();
	}

	@Test
	void loginSavesRefreshTokenWithExpiry() {
		SignupResponse mom = signupMom();

		LoginResponse response = authService.login(new LoginRequest("mom@example.com", "password123"));

		List<RefreshToken> saved = refreshTokenRepository.findAll();
		assertThat(saved).hasSize(1);
		assertThat(saved.get(0).getToken()).isEqualTo(response.refreshToken());
		assertThat(saved.get(0).getUser().getId()).isEqualTo(mom.id());
		assertThat(saved.get(0).getExpiresAt()).isCloseTo(
				LocalDateTime.now().plus(jwtProperties.refreshExpiration()), within(1, ChronoUnit.MINUTES));
	}

	@Test
	void loginTwiceSavesTwoDifferentRefreshTokens() {
		signupMom();

		LoginResponse first = authService.login(new LoginRequest("mom@example.com", "password123"));
		LoginResponse second = authService.login(new LoginRequest("mom@example.com", "password123"));

		assertThat(first.refreshToken()).isNotEqualTo(second.refreshToken());
		assertThat(refreshTokenRepository.count()).isEqualTo(2);
	}

	@Test
	void loginRejectsUnknownEmail() {
		signupMom();

		assertThatThrownBy(() -> authService.login(new LoginRequest("dad@example.com", "password123")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.LOGIN_FAILED);
		assertThat(refreshTokenRepository.count()).isZero();
	}

	@Test
	void loginRejectsWrongPassword() {
		signupMom();

		assertThatThrownBy(() -> authService.login(new LoginRequest("mom@example.com", "wrong-password")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.LOGIN_FAILED);
		assertThat(refreshTokenRepository.count()).isZero();
	}

	@Test
	void loginRejectsSuspendedUser() {
		changeStatus(signupMom().id(), UserStatus.SUSPENDED);

		assertThatThrownBy(() -> authService.login(new LoginRequest("mom@example.com", "password123")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_SUSPENDED);
		assertThat(refreshTokenRepository.count()).isZero();
	}

	@Test
	void loginDoesNotRevealSuspensionForWrongPassword() {
		changeStatus(signupMom().id(), UserStatus.SUSPENDED);

		assertThatThrownBy(() -> authService.login(new LoginRequest("mom@example.com", "wrong-password")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.LOGIN_FAILED);
		assertThat(refreshTokenRepository.count()).isZero();
	}

	@Test
	void loginRejectsWithdrawnUser() {
		changeStatus(signupMom().id(), UserStatus.WITHDRAWN);

		assertThatThrownBy(() -> authService.login(new LoginRequest("mom@example.com", "password123")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.LOGIN_FAILED);
		assertThat(refreshTokenRepository.count()).isZero();
	}

	@Test
	void reissueReturnsAccessTokenOfUser() {
		String oldToken = loginMom();

		ReissueResponse response = authService.reissue(new ReissueRequest(oldToken));

		AccessTokenClaims claims = jwtProvider.parseAccessToken(response.accessToken());
		assertThat(claims.userId()).isEqualTo(userRepository.findByEmail("mom@example.com").orElseThrow().getId());
		assertThat(claims.role()).isEqualTo("USER");
	}

	@Test
	void reissueReturnsNewRefreshToken() {
		String oldToken = loginMom();

		ReissueResponse response = authService.reissue(new ReissueRequest(oldToken));

		assertThat(response.refreshToken()).isNotEqualTo(oldToken);
		assertThat(jwtProvider.parseRefreshToken(response.refreshToken()))
				.isEqualTo(jwtProvider.parseRefreshToken(oldToken));
	}

	@Test
	void reissueReplacesStoredRefreshToken() {
		String oldToken = loginMom();

		ReissueResponse response = authService.reissue(new ReissueRequest(oldToken));

		assertThat(refreshTokenRepository.findAll())
				.extracting(RefreshToken::getToken)
				.containsExactly(response.refreshToken());
	}

	@Test
	void reissueRejectsAlreadyUsedToken() {
		String oldToken = loginMom();

		authService.reissue(new ReissueRequest(oldToken));

		assertThatThrownBy(() -> authService.reissue(new ReissueRequest(oldToken)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	@Test
	void reissueRejectsTokenThatWasNeverStored() {
		String notStored = jwtProvider.createRefreshToken(signupMom().id());

		assertThatThrownBy(() -> authService.reissue(new ReissueRequest(notStored)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	@Test
	void reissueRejectsMalformedToken() {
		assertThatThrownBy(() -> authService.reissue(new ReissueRequest("not-a-token")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	@Test
	void reissueRejectsAccessToken() {
		String accessToken = jwtProvider.createAccessToken(signupMom().id(), "USER");

		assertThatThrownBy(() -> authService.reissue(new ReissueRequest(accessToken)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	@Test
	void reissueRejectsExpiredToken() {
		User user = userRepository.findById(signupMom().id()).orElseThrow();
		JwtProvider expiredProvider = new JwtProvider(new JwtProperties(jwtProperties.secret(),
				jwtProperties.accessExpiration(), Duration.ofSeconds(-1)));
		String expired = expiredProvider.createRefreshToken(user.getId());
		refreshTokenRepository.saveAndFlush(new RefreshToken(user, expired, LocalDateTime.now().minusSeconds(1)));

		assertThatThrownBy(() -> authService.reissue(new ReissueRequest(expired)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.TOKEN_EXPIRED);
	}

	@Test
	void reissueRejectsSuspendedUser() {
		String oldToken = loginMom();

		changeStatus(jwtProvider.parseRefreshToken(oldToken), UserStatus.SUSPENDED);

		assertThatThrownBy(() -> authService.reissue(new ReissueRequest(oldToken)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_SUSPENDED);
	}

	@Test
	void reissueRejectsWithdrawnUser() {
		String oldToken = loginMom();

		changeStatus(jwtProvider.parseRefreshToken(oldToken), UserStatus.WITHDRAWN);

		assertThatThrownBy(() -> authService.reissue(new ReissueRequest(oldToken)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	@Test
	void logoutDeletesRefreshToken() {
		String token = loginMom();

		authService.logout(jwtProvider.parseRefreshToken(token), new LogoutRequest(token));

		assertThat(refreshTokenRepository.count()).isZero();
	}

	@Test
	void loggedOutTokenCannotBeReissued() {
		String token = loginMom();
		authService.logout(jwtProvider.parseRefreshToken(token), new LogoutRequest(token));

		assertThatThrownBy(() -> authService.reissue(new ReissueRequest(token)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	@Test
	void logoutKeepsTokensOfOtherDevices() {
		String token = loginMom();
		String otherDevice = authService.login(new LoginRequest("mom@example.com", "password123")).refreshToken();

		authService.logout(jwtProvider.parseRefreshToken(token), new LogoutRequest(token));

		assertThat(refreshTokenRepository.findAll())
				.extracting(RefreshToken::getToken)
				.containsExactly(otherDevice);
	}

	@Test
	void logoutIgnoresTokenOfAnotherUser() {
		String token = loginMom();
		Long dadId = authService.signup(new SignupRequest("dad@example.com", "password123", "김아빠")).id();

		authService.logout(dadId, new LogoutRequest(token));

		assertThat(refreshTokenRepository.count()).isEqualTo(1);
	}

	@Test
	void logoutWithUnknownTokenDoesNothing() {
		String token = loginMom();

		authService.logout(jwtProvider.parseRefreshToken(token), new LogoutRequest("unknown-token"));

		assertThat(refreshTokenRepository.count()).isEqualTo(1);
	}

	@Test
	void adminLoginReturnsAccessTokenWithAdminRole() {
		Long adminId = signupAdmin();

		AdminLoginResponse response = authService.adminLogin(new LoginRequest("mom@example.com", "password123"));

		AccessTokenClaims claims = jwtProvider.parseAccessToken(response.accessToken());
		assertThat(claims.userId()).isEqualTo(adminId);
		assertThat(claims.role()).isEqualTo("ADMIN");
	}

	@Test
	void adminLoginReturnsAdminInfo() {
		Long adminId = signupAdmin();

		AdminLoginResponse response = authService.adminLogin(new LoginRequest("mom@example.com", "password123"));

		assertThat(response.admin().id()).isEqualTo(adminId);
		assertThat(response.admin().name()).isEqualTo("김엄마");
		assertThat(response.admin().adminLevel()).isEqualTo(AdminLevel.SUPER);
	}

	@Test
	void adminLoginSavesRefreshToken() {
		signupAdmin();

		AdminLoginResponse response = authService.adminLogin(new LoginRequest("mom@example.com", "password123"));

		assertThat(refreshTokenRepository.findAll())
				.extracting(RefreshToken::getToken)
				.containsExactly(response.refreshToken());
	}

	@Test
	void adminLoginRejectsUserWhoIsNotAdmin() {
		signupMom();

		assertThatThrownBy(() -> authService.adminLogin(new LoginRequest("mom@example.com", "password123")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.LOGIN_FAILED);
		assertThat(refreshTokenRepository.count()).isZero();
	}

	@Test
	void adminLoginRejectsWrongPassword() {
		signupAdmin();

		assertThatThrownBy(() -> authService.adminLogin(new LoginRequest("mom@example.com", "wrong-password")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.LOGIN_FAILED);
		assertThat(refreshTokenRepository.count()).isZero();
	}

	private Long signupAdmin() {
		User user = userRepository.findById(signupMom().id()).orElseThrow();
		ReflectionTestUtils.setField(user, "role", Role.ADMIN);
		ReflectionTestUtils.setField(user, "adminLevel", AdminLevel.SUPER);
		userRepository.flush();
		return user.getId();
	}

	private String loginMom() {
		signupMom();
		return authService.login(new LoginRequest("mom@example.com", "password123")).refreshToken();
	}

	private SignupResponse signupMom() {
		return authService.signup(new SignupRequest("mom@example.com", "password123", "김엄마"));
	}

	private void changeStatus(Long userId, UserStatus status) {
		User user = userRepository.findById(userId).orElseThrow();
		ReflectionTestUtils.setField(user, "status", status);
		userRepository.flush();
	}

}
