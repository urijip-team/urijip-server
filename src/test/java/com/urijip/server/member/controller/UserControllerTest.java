package com.urijip.server.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.LocalDate;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.member.dto.request.DeviceTokenRequest;
import com.urijip.server.member.dto.request.PaymentInfoRequest;
import com.urijip.server.member.dto.request.ProfileUpdateRequest;
import com.urijip.server.member.dto.response.AccountNumberResponse;
import com.urijip.server.member.dto.response.PaymentInfoResponse;
import com.urijip.server.member.dto.response.ProfileResponse;
import com.urijip.server.member.entity.Platform;
import com.urijip.server.member.service.AuthService;
import com.urijip.server.member.service.DeviceTokenService;
import com.urijip.server.member.service.PaymentInfoService;
import com.urijip.server.member.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private DeviceTokenService deviceTokenService;

	@MockitoBean
	private ProfileService profileService;

	@MockitoBean
	private PaymentInfoService paymentInfoService;

	@Test
	void getProfileReturnsProfile() {
		given(profileService.getProfile(2L, 1L)).willReturn(new ProfileResponse(1L, "김엄마",
				"https://example.com/mom.png", LocalDate.of(1980, 5, 5), "오늘도 화이팅"));

		MvcTestResult result = getProfile(2L, "1");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo("김엄마");
		assertThat(result).bodyJson().extractingPath("$.data.profileImageUrl").isEqualTo("https://example.com/mom.png");
		assertThat(result).bodyJson().extractingPath("$.data.birthday").isEqualTo("1980-05-05");
		assertThat(result).bodyJson().extractingPath("$.data.statusMessage").isEqualTo("오늘도 화이팅");
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	@Test
	void getProfileDoesNotExposeEmailOrPassword() {
		given(profileService.getProfile(2L, 1L)).willReturn(new ProfileResponse(1L, "김엄마", null, null, null));

		MvcTestResult result = getProfile(2L, "1");

		assertThat(result).bodyJson().doesNotHavePath("$.data.email");
		assertThat(result).bodyJson().doesNotHavePath("$.data.password");
		assertThat(result).bodyJson().extractingPath("$.data.birthday").isNull();
	}

	@Test
	void getProfileOfUserOutsideFamilyReturnsForbidden() {
		given(profileService.getProfile(any(), any())).willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = getProfile(2L, "1");

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void getProfileWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.get().uri("/api/users/1").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(profileService).should(never()).getProfile(any(), any());
	}

	@Test
	void getProfileWithNonNumericUserIdReturnsBadRequest() {
		MvcTestResult result = getProfile(2L, "abc");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		then(profileService).should(never()).getProfile(any(), any());
	}

	@Test
	void updateMyProfileReturnsUpdatedProfile() {
		given(profileService.updateMyProfile(1L,
				new ProfileUpdateRequest("김영희", LocalDate.of(1980, 5, 5), "오늘도 화이팅")))
				.willReturn(new ProfileResponse(1L, "김영희", null, LocalDate.of(1980, 5, 5), "오늘도 화이팅"));

		MvcTestResult result = updateMyProfile(1L, """
				{"name": "김영희", "birthday": "1980-05-05", "statusMessage": "오늘도 화이팅"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data.name").isEqualTo("김영희");
		assertThat(result).bodyJson().extractingPath("$.data.birthday").isEqualTo("1980-05-05");
		assertThat(result).bodyJson().extractingPath("$.data.statusMessage").isEqualTo("오늘도 화이팅");
	}

	@Test
	void updateMyProfileAcceptsPartialBody() {
		updateMyProfile(7L, """
				{"statusMessage": "퇴근 중"}
				""");

		then(profileService).should().updateMyProfile(7L, new ProfileUpdateRequest(null, null, "퇴근 중"));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			name          | {"name": " "}
			name          | {"name": "가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가가"}
			birthday      | {"birthday": "2999-01-01"}
			""")
	void updateMyProfileWithInvalidInputReturnsBadRequest(String field, String body) {
		MvcTestResult result = updateMyProfile(1L, body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains(field + ": ");
		then(profileService).should(never()).updateMyProfile(any(), any());
	}

	@Test
	void updateMyProfileWithTooLongStatusMessageReturnsBadRequest() {
		MvcTestResult result = updateMyProfile(1L, """
				{"statusMessage": "%s"}
				""".formatted("가".repeat(101)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.message")
				.isEqualTo("statusMessage: 100자 이하여야 합니다");
	}

	@Test
	void updateMyProfileWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.patch().uri("/api/users/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "김영희"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(profileService).should(never()).updateMyProfile(any(), any());
	}

	@Test
	void withdrawReturnsOkWithoutData() {
		MvcTestResult result = withdraw(1L);

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data").isNull();
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	@Test
	void withdrawPassesUserIdOfAccessToken() {
		withdraw(7L);

		then(authService).should().withdraw(7L);
	}

	@Test
	void withdrawOfOwnerWithOtherMembersReturnsConflict() {
		willThrow(new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE)).given(authService).withdraw(1L);

		MvcTestResult result = withdraw(1L);

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("OWNER_CANNOT_LEAVE");
	}

	@Test
	void withdrawWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.delete().uri("/api/users/me").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(authService).should(never()).withdraw(any());
	}

	@Test
	void registerDeviceTokenReturnsOkWithoutData() {
		MvcTestResult result = registerDeviceToken(1L, """
				{"fcmToken": "fcm-1", "platform": "ANDROID"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data").isNull();
		assertThat(result).bodyJson().extractingPath("$.error").isNull();
	}

	@Test
	void registerDeviceTokenPassesUserIdAndRequest() {
		registerDeviceToken(7L, """
				{"fcmToken": "fcm-1", "platform": "IOS"}
				""");

		then(deviceTokenService).should().register(7L, new DeviceTokenRequest("fcm-1", Platform.IOS));
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"platform\": \"IOS\"}", "{\"fcmToken\": \" \", \"platform\": \"IOS\"}"})
	void registerDeviceTokenWithoutFcmTokenReturnsBadRequest(String body) {
		MvcTestResult result = registerDeviceToken(1L, body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains("fcmToken: ");
		then(deviceTokenService).should(never()).register(any(), any());
	}

	@Test
	void registerDeviceTokenWithoutPlatformReturnsBadRequest() {
		MvcTestResult result = registerDeviceToken(1L, """
				{"fcmToken": "fcm-1"}
				""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains("platform: ");
		then(deviceTokenService).should(never()).register(any(), any());
	}

	@Test
	void registerDeviceTokenWithUnknownPlatformReturnsBadRequest() {
		MvcTestResult result = registerDeviceToken(1L, """
				{"fcmToken": "fcm-1", "platform": "WINDOWS"}
				""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		then(deviceTokenService).should(never()).register(any(), any());
	}

	@Test
	void registerDeviceTokenWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.put().uri("/api/users/me/device-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fcmToken": "fcm-1", "platform": "ANDROID"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(deviceTokenService).should(never()).register(any(), any());
	}

	@Test
	void registerPaymentInfoReturnsMaskedPaymentInfo() {
		given(paymentInfoService.register(1L, new PaymentInfoRequest("국민은행", "12345678905678", "김엄마",
				"https://toss.me/example")))
				.willReturn(new PaymentInfoResponse("국민은행", "****-5678", "김엄마", "https://toss.me/example"));

		MvcTestResult result = registerPaymentInfo(1L, """
				{"bankName": "국민은행", "accountNumber": "12345678905678", "accountHolder": "김엄마",
				 "transferLink": "https://toss.me/example"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.bankName").isEqualTo("국민은행");
		assertThat(result).bodyJson().extractingPath("$.data.maskedAccount").isEqualTo("****-5678");
		assertThat(result).bodyJson().extractingPath("$.data.accountHolder").isEqualTo("김엄마");
		assertThat(result).bodyJson().extractingPath("$.data.transferLink").isEqualTo("https://toss.me/example");
		assertThat(result).bodyJson().doesNotHavePath("$.data.accountNumber");
	}

	@ParameterizedTest
	@ValueSource(strings = {"https://toss.me/example", "https://qr.kakaopay.com/FABCDE123"})
	void registerPaymentInfoAcceptsTossAndKakaoPayLinks(String link) {
		registerPaymentInfo(7L, """
				{"bankName": "국민은행", "accountNumber": "1234567890", "accountHolder": "김엄마", "transferLink": "%s"}
				""".formatted(link));

		then(paymentInfoService).should().register(7L, new PaymentInfoRequest("국민은행", "1234567890", "김엄마", link));
	}

	@Test
	void registerPaymentInfoAcceptsMissingTransferLink() {
		registerPaymentInfo(7L, """
				{"bankName": "국민은행", "accountNumber": "1234567890", "accountHolder": "김엄마"}
				""");

		then(paymentInfoService).should().register(7L, new PaymentInfoRequest("국민은행", "1234567890", "김엄마", null));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			bankName      | {"bankName": " ", "accountNumber": "1234567890", "accountHolder": "김엄마"}
			accountNumber | {"bankName": "국민은행", "accountHolder": "김엄마"}
			accountNumber | {"bankName": "국민은행", "accountNumber": "123-456-7890", "accountHolder": "김엄마"}
			accountNumber | {"bankName": "국민은행", "accountNumber": "123456789", "accountHolder": "김엄마"}
			accountNumber | {"bankName": "국민은행", "accountNumber": "12345678901234567", "accountHolder": "김엄마"}
			accountHolder | {"bankName": "국민은행", "accountNumber": "1234567890"}
			transferLink  | {"bankName": "국민은행", "accountNumber": "1234567890", "accountHolder": "김엄마", "transferLink": "https://evil.com/pay"}
			transferLink  | {"bankName": "국민은행", "accountNumber": "1234567890", "accountHolder": "김엄마", "transferLink": "http://toss.me/example"}
			transferLink  | {"bankName": "국민은행", "accountNumber": "1234567890", "accountHolder": "김엄마", "transferLink": "https://toss.me.evil.com/x"}
			transferLink  | {"bankName": "국민은행", "accountNumber": "1234567890", "accountHolder": "김엄마", "transferLink": "https://toss.me/"}
			""")
	void registerPaymentInfoWithInvalidInputReturnsBadRequest(String field, String body) {
		MvcTestResult result = registerPaymentInfo(1L, body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains(field + ": ");
		then(paymentInfoService).should(never()).register(any(), any());
	}

	@Test
	void registerPaymentInfoWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.put().uri("/api/users/me/payment-info")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"bankName": "국민은행", "accountNumber": "1234567890", "accountHolder": "김엄마"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_TOKEN");
		then(paymentInfoService).should(never()).register(any(), any());
	}

	@Test
	void getPaymentInfoReturnsMaskedPaymentInfo() {
		given(paymentInfoService.getPaymentInfo(2L, 1L))
				.willReturn(new PaymentInfoResponse("국민은행", "****-5678", "김엄마", null));

		MvcTestResult result = mvc.get().uri("/api/users/1/payment-info")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(2L, "USER"))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.maskedAccount").isEqualTo("****-5678");
		assertThat(result).bodyJson().extractingPath("$.data.transferLink").isNull();
	}

	@Test
	void getPaymentInfoWithoutRegisteredInfoReturnsNotFound() {
		given(paymentInfoService.getPaymentInfo(any(), any()))
				.willThrow(new BusinessException(ErrorCode.PAYMENT_INFO_NOT_FOUND));

		MvcTestResult result = mvc.get().uri("/api/users/1/payment-info")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(2L, "USER"))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("PAYMENT_INFO_NOT_FOUND");
	}

	@Test
	void getAccountNumberReturnsFullNumber() {
		given(paymentInfoService.getAccountNumber(2L, 1L)).willReturn(new AccountNumberResponse("12345678905678"));

		MvcTestResult result = mvc.get().uri("/api/users/1/payment-info/account")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(2L, "USER"))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.accountNumber").isEqualTo("12345678905678");
	}

	@Test
	void getAccountNumberOfUserOutsideFamilyReturnsForbidden() {
		given(paymentInfoService.getAccountNumber(any(), any()))
				.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = mvc.get().uri("/api/users/1/payment-info/account")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(2L, "USER"))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void getAccountNumberWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.get().uri("/api/users/1/payment-info/account").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		then(paymentInfoService).should(never()).getAccountNumber(any(), any());
	}

	private MvcTestResult getProfile(Long userId, String targetUserId) {
		return mvc.get().uri("/api/users/{userId}", targetUserId)
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.exchange();
	}

	private MvcTestResult updateMyProfile(Long userId, String body) {
		return mvc.patch().uri("/api/users/me")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult withdraw(Long userId) {
		return mvc.delete().uri("/api/users/me")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.exchange();
	}

	private MvcTestResult registerDeviceToken(Long userId, String body) {
		return mvc.put().uri("/api/users/me/device-token")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult registerPaymentInfo(Long userId, String body) {
		return mvc.put().uri("/api/users/me/payment-info")
				.header("Authorization", "Bearer " + jwtProvider.createAccessToken(userId, "USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

}
