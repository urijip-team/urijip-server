package com.urijip.server.allowance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.urijip.server.allowance.dto.request.AllowanceCreateRequest;
import com.urijip.server.allowance.dto.request.AllowanceStatusRequest;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

import com.urijip.server.allowance.dto.response.AllowanceCreateResponse;
import com.urijip.server.allowance.dto.response.AllowanceDetailResponse;
import com.urijip.server.allowance.dto.response.AllowanceMonthlyResponse;
import com.urijip.server.allowance.dto.response.AllowanceResponse;
import com.urijip.server.allowance.entity.AllowanceStatus;
import com.urijip.server.allowance.service.AllowanceService;
import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.member.dto.response.PaymentInfoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(AllowanceController.class)
@Import(SecurityConfig.class)
class AllowanceControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AllowanceService allowanceService;

	@Test
	void createReturnsCreated() {
		given(allowanceService.create(2L, new AllowanceCreateRequest(3L, 1L, 30000, "문제집 사야 해요")))
				.willReturn(new AllowanceCreateResponse(45L, AllowanceStatus.REQUESTED));

		MvcTestResult result = create("""
				{"familyId": 3, "receiverId": 1, "amount": 30000, "reason": "문제집 사야 해요"}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.data.requestId").isEqualTo(45);
		assertThat(result).bodyJson().extractingPath("$.data.status").isEqualTo("REQUESTED");
	}

	@Test
	void createWithoutReasonIsAllowed() {
		given(allowanceService.create(2L, new AllowanceCreateRequest(3L, 1L, 5000, null)))
				.willReturn(new AllowanceCreateResponse(46L, AllowanceStatus.REQUESTED));

		MvcTestResult result = create("{\"familyId\": 3, \"receiverId\": 1, \"amount\": 5000}");

		assertThat(result).hasStatus(HttpStatus.CREATED);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"receiverId\": 1, \"amount\": 5000}",
			"{\"familyId\": 3, \"amount\": 5000}",
			"{\"familyId\": 3, \"receiverId\": 1}",
			"{\"familyId\": 3, \"receiverId\": 1, \"amount\": 0}",
			"{\"familyId\": 3, \"receiverId\": 1, \"amount\": -100}",
			"{\"familyId\": 3, \"receiverId\": 1, \"amount\": 10000001}"
	})
	void createWithInvalidBodyReturnsBadRequest(String body) {
		MvcTestResult result = create(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(allowanceService).should(never()).create(any(), any());
	}

	@Test
	void createWithTooLongReasonReturnsBadRequest() {
		MvcTestResult result = create("{\"familyId\": 3, \"receiverId\": 1, \"amount\": 5000, \"reason\": \"%s\"}"
				.formatted("가".repeat(101)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void createToSelfReturnsBadRequest() {
		given(allowanceService.create(2L, new AllowanceCreateRequest(3L, 2L, 5000, null)))
				.willThrow(new BusinessException(ErrorCode.CANNOT_REQUEST_SELF));

		MvcTestResult result = create("{\"familyId\": 3, \"receiverId\": 2, \"amount\": 5000}");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("CANNOT_REQUEST_SELF");
	}

	@Test
	void createInOtherFamilyReturnsForbidden() {
		given(allowanceService.create(2L, new AllowanceCreateRequest(9L, 1L, 5000, null)))
				.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = create("{\"familyId\": 9, \"receiverId\": 1, \"amount\": 5000}");

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void createWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/allowance-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"familyId\": 3, \"receiverId\": 1, \"amount\": 5000}")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void getDetailReturnsRequestWithPaymentInfo() {
		given(allowanceService.getDetail(45L, 2L)).willReturn(new AllowanceDetailResponse(45L, 3L,
				new AllowanceResponse.Person(2L, "김첫째"), new AllowanceResponse.Person(1L, "김엄마"), 30000, "문제집 사야 해요",
				AllowanceStatus.REQUESTED, OffsetDateTime.parse("2026-11-08T10:00:00+09:00"), null,
				new PaymentInfoResponse("국민은행", "****-5678", "김첫째", "https://toss.me/kid")));

		MvcTestResult result = mvc.get().uri("/api/allowance-requests/45").header("Authorization", kid()).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(45);
		assertThat(result).bodyJson().extractingPath("$.data.familyId").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.data.requester.name").isEqualTo("김첫째");
		assertThat(result).bodyJson().extractingPath("$.data.receiver.id").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.data.amount").isEqualTo(30000);
		assertThat(result).bodyJson().extractingPath("$.data.status").isEqualTo("REQUESTED");
		assertThat(result).bodyJson().extractingPath("$.data.createdAt").isEqualTo("2026-11-08T10:00:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data.completedAt").isNull();
		assertThat(result).bodyJson().extractingPath("$.data.requesterPaymentInfo.maskedAccount").isEqualTo("****-5678");
		assertThat(result).bodyJson().extractingPath("$.data.requesterPaymentInfo.transferLink")
				.isEqualTo("https://toss.me/kid");
	}

	@Test
	void getDetailByOtherMemberReturnsForbidden() {
		given(allowanceService.getDetail(45L, 2L))
				.willThrow(new BusinessException(ErrorCode.NOT_ALLOWANCE_PARTICIPANT));

		MvcTestResult result = mvc.get().uri("/api/allowance-requests/45").header("Authorization", kid()).exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("NOT_ALLOWANCE_PARTICIPANT");
	}

	@Test
	void getUnknownDetailReturnsNotFound() {
		given(allowanceService.getDetail(45L, 2L)).willThrow(new BusinessException(ErrorCode.ALLOWANCE_NOT_FOUND));

		MvcTestResult result = mvc.get().uri("/api/allowance-requests/45").header("Authorization", kid()).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("ALLOWANCE_NOT_FOUND");
	}

	@Test
	void changeStatusReturnsCompletedRequest() {
		given(allowanceService.changeStatus(45L, 1L, new AllowanceStatusRequest(AllowanceStatus.SENT)))
				.willReturn(new AllowanceResponse(45L, 3L, new AllowanceResponse.Person(2L, "김첫째"),
						new AllowanceResponse.Person(1L, "김엄마"), 30000, null, AllowanceStatus.SENT,
						OffsetDateTime.parse("2026-11-08T10:00:00+09:00"), OffsetDateTime.parse("2026-11-08T12:30:00+09:00")));

		MvcTestResult result = changeStatus("{\"status\": \"SENT\"}", mom());

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.status").isEqualTo("SENT");
		assertThat(result).bodyJson().extractingPath("$.data.completedAt").isEqualTo("2026-11-08T12:30:00+09:00");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"status\": null}"})
	void changeStatusWithInvalidBodyReturnsBadRequest(String body) {
		MvcTestResult result = changeStatus(body, mom());

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		then(allowanceService).should(never()).changeStatus(any(), any(), any());
	}

	@Test
	void changeStatusWithUnknownStatusReturnsBadRequest() {
		MvcTestResult result = changeStatus("{\"status\": \"DONE\"}", mom());

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(allowanceService).should(never()).changeStatus(any(), any(), any());
	}

	@Test
	void changeStatusByRequesterReturnsForbidden() {
		given(allowanceService.changeStatus(45L, 2L, new AllowanceStatusRequest(AllowanceStatus.SENT)))
				.willThrow(new BusinessException(ErrorCode.NOT_ALLOWANCE_RECEIVER));

		MvcTestResult result = changeStatus("{\"status\": \"SENT\"}", kid());

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("NOT_ALLOWANCE_RECEIVER");
	}

	@Test
	void changeStatusTwiceReturnsConflict() {
		given(allowanceService.changeStatus(45L, 1L, new AllowanceStatusRequest(AllowanceStatus.REJECTED)))
				.willThrow(new BusinessException(ErrorCode.ALLOWANCE_ALREADY_HANDLED));

		MvcTestResult result = changeStatus("{\"status\": \"REJECTED\"}", mom());

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("ALLOWANCE_ALREADY_HANDLED");
	}

	@Test
	void getMonthlyReturnsRequestsAndTotal() {
		given(allowanceService.getMonthly(2L, YearMonth.of(2026, 10), "sent")).willReturn(new AllowanceMonthlyResponse(
				List.of(new AllowanceResponse(45L, 3L, new AllowanceResponse.Person(2L, "김첫째"),
						new AllowanceResponse.Person(1L, "김엄마"), 30000, "문제집", AllowanceStatus.SENT,
						OffsetDateTime.parse("2026-10-08T10:00:00+09:00"),
						OffsetDateTime.parse("2026-10-08T12:30:00+09:00"))), 30000));

		MvcTestResult result = mvc.get().uri("/api/allowance-requests?month=2026-10&type=sent")
				.header("Authorization", kid())
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.requests[0].id").isEqualTo(45);
		assertThat(result).bodyJson().extractingPath("$.data.requests[0].status").isEqualTo("SENT");
		assertThat(result).bodyJson().extractingPath("$.data.totalAmount").isEqualTo(30000);
	}

	@ParameterizedTest
	@ValueSource(strings = {"?type=sent", "?month=2026-10", "?month=2026-13&type=sent", "?month=202610&type=sent"})
	void getMonthlyWithMissingOrInvalidParamReturnsBadRequest(String query) {
		MvcTestResult result = mvc.get().uri("/api/allowance-requests" + query).header("Authorization", kid()).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		then(allowanceService).should(never()).getMonthly(any(), any(), any());
	}

	@Test
	void getMonthlyWithUnknownTypeReturnsBadRequest() {
		given(allowanceService.getMonthly(2L, YearMonth.of(2026, 10), "all"))
				.willThrow(new BusinessException(ErrorCode.INVALID_INPUT));

		MvcTestResult result = mvc.get().uri("/api/allowance-requests?month=2026-10&type=all")
				.header("Authorization", kid())
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
	}

	@Test
	void getMonthlyWithoutAccessTokenReturnsUnauthorized() {
		assertThat(mvc.get().uri("/api/allowance-requests?month=2026-10&type=sent").exchange())
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private MvcTestResult changeStatus(String body, String authorization) {
		return mvc.patch().uri("/api/allowance-requests/45")
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String mom() {
		return "Bearer " + jwtProvider.createAccessToken(1L, "USER");
	}

	private MvcTestResult create(String body) {
		return mvc.post().uri("/api/allowance-requests")
				.header("Authorization", kid())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private String kid() {
		return "Bearer " + jwtProvider.createAccessToken(2L, "USER");
	}

}
