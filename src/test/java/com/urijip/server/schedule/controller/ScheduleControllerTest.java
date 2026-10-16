package com.urijip.server.schedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import com.urijip.server.global.config.SecurityConfig;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.schedule.dto.request.ScheduleCreateRequest;
import com.urijip.server.schedule.dto.request.ScheduleUpdateRequest;
import com.urijip.server.schedule.dto.response.BirthdayResponse;
import com.urijip.server.schedule.dto.response.MonthlyScheduleResponse;
import com.urijip.server.schedule.dto.response.ScheduleResponse;
import com.urijip.server.schedule.service.ScheduleService;
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

@WebMvcTest(ScheduleController.class)
@Import(SecurityConfig.class)
class ScheduleControllerTest {

	private static final OffsetDateTime START = OffsetDateTime.of(2026, 10, 2, 14, 0, 0, 0, ZoneOffset.ofHours(9));
	private static final OffsetDateTime END = START.plusHours(2);
	// Jackson은 요청의 날짜를 UTC로 맞춰 읽는다. 같은 시각이고 서비스가 서울 시간으로 바꾼다.
	private static final OffsetDateTime START_UTC = START.withOffsetSameInstant(ZoneOffset.UTC);
	private static final OffsetDateTime END_UTC = END.withOffsetSameInstant(ZoneOffset.UTC);
	private static final ScheduleResponse SCHEDULE = new ScheduleResponse(5L, "장보기", "우유", START, END, false,
			List.of(1L, 2L), 1L);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private ScheduleService scheduleService;

	@Test
	void getMonthlyReturnsSchedulesAndBirthdays() {
		given(scheduleService.getMonthly(3L, 1L, YearMonth.of(2026, 10))).willReturn(new MonthlyScheduleResponse(
				List.of(SCHEDULE), List.of(new BirthdayResponse(2L, "김아빠", LocalDate.of(2026, 10, 3)))));

		MvcTestResult result = mvc.get().uri("/api/families/3/schedules?month=2026-10")
				.header("Authorization", bearer(1L))
				.exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.schedules[0].id").isEqualTo(5);
		assertThat(result).bodyJson().extractingPath("$.data.schedules[0].startAt")
				.isEqualTo("2026-10-02T14:00:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data.schedules[0].participantIds").isEqualTo(List.of(1, 2));
		assertThat(result).bodyJson().extractingPath("$.data.birthdays[0].userId").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.data.birthdays[0].name").isEqualTo("김아빠");
		assertThat(result).bodyJson().extractingPath("$.data.birthdays[0].date").isEqualTo("2026-10-03");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "?month=2026-13", "?month=202610", "?month=abc"})
	void getMonthlyWithoutValidMonthReturnsBadRequest(String query) {
		MvcTestResult result = mvc.get().uri("/api/families/3/schedules" + query)
				.header("Authorization", bearer(1L))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false);
		then(scheduleService).should(never()).getMonthly(any(), any(), any());
	}

	@Test
	void getMonthlyOfOtherFamilyReturnsForbidden() {
		given(scheduleService.getMonthly(any(), any(), any()))
				.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER));

		MvcTestResult result = mvc.get().uri("/api/families/3/schedules?month=2026-10")
				.header("Authorization", bearer(1L))
				.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void createReturnsCreatedSchedule() {
		given(scheduleService.create(3L, 1L,
				new ScheduleCreateRequest("장보기", "우유", START_UTC, END_UTC, null, List.of(1L, 2L))))
				.willReturn(SCHEDULE);

		MvcTestResult result = create(1L, """
				{"title": "장보기", "memo": "우유", "startAt": "2026-10-02T14:00:00+09:00",
				 "endAt": "2026-10-02T16:00:00+09:00", "participantIds": [1, 2]}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(5);
		assertThat(result).bodyJson().extractingPath("$.data.endAt").isEqualTo("2026-10-02T16:00:00+09:00");
		assertThat(result).bodyJson().extractingPath("$.data.allDay").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.data.createdBy").isEqualTo(1);
	}

	@Test
	void createAcceptsMinimalBody() {
		create(7L, """
				{"title": "장보기", "startAt": "2026-10-02T14:00:00+09:00", "endAt": "2026-10-02T14:00:00+09:00"}
				""");

		then(scheduleService).should().create(3L, 7L, new ScheduleCreateRequest("장보기", null, START_UTC, START_UTC,
				null, null));
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			title       | {"title": " ", "startAt": "2026-10-02T14:00:00+09:00", "endAt": "2026-10-02T16:00:00+09:00"}
			title       | {"startAt": "2026-10-02T14:00:00+09:00", "endAt": "2026-10-02T16:00:00+09:00"}
			startAt     | {"title": "장보기", "endAt": "2026-10-02T16:00:00+09:00"}
			endAt       | {"title": "장보기", "startAt": "2026-10-02T14:00:00+09:00"}
			validPeriod | {"title": "장보기", "startAt": "2026-10-02T14:00:00+09:00", "endAt": "2026-10-02T13:59:00+09:00"}
			""")
	void createWithInvalidInputReturnsBadRequest(String field, String body) {
		MvcTestResult result = create(1L, body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_INPUT");
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains(field + ": ");
		then(scheduleService).should(never()).create(any(), any(), any());
	}

	@Test
	void createWithTooLongTitleReturnsBadRequest() {
		MvcTestResult result = create(1L, """
				{"title": "%s", "startAt": "2026-10-02T14:00:00+09:00", "endAt": "2026-10-02T16:00:00+09:00"}
				""".formatted("가".repeat(51)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.message").isEqualTo("title: 50자 이하여야 합니다");
	}

	@Test
	void createWithParticipantOutsideFamilyReturnsNotFound() {
		given(scheduleService.create(any(), any(), any()))
				.willThrow(new BusinessException(ErrorCode.FAMILY_MEMBER_NOT_FOUND));

		MvcTestResult result = create(1L, """
				{"title": "장보기", "startAt": "2026-10-02T14:00:00+09:00", "endAt": "2026-10-02T16:00:00+09:00",
				 "participantIds": [99]}
				""");

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_MEMBER_NOT_FOUND");
	}

	@Test
	void createWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.post().uri("/api/families/3/schedules")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title": "장보기", "startAt": "2026-10-02T14:00:00+09:00", "endAt": "2026-10-02T16:00:00+09:00"}
						""")
				.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		then(scheduleService).should(never()).create(any(), any(), any());
	}

	@Test
	void updateReturnsUpdatedSchedule() {
		given(scheduleService.update(5L, 1L, new ScheduleUpdateRequest("마트", null, null, null, null, null)))
				.willReturn(SCHEDULE);

		MvcTestResult result = update(1L, """
				{"title": "마트"}
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.data.id").isEqualTo(5);
	}

	@Test
	void updatePassesAllFields() {
		update(7L, """
				{"title": "마트", "memo": "", "startAt": "2026-10-02T14:00:00+09:00",
				 "endAt": "2026-10-02T16:00:00+09:00", "allDay": true, "participantIds": []}
				""");

		then(scheduleService).should().update(5L, 7L, new ScheduleUpdateRequest("마트", "", START_UTC, END_UTC, true,
				List.of()));
	}

	@Test
	void updateWithBlankTitleReturnsBadRequest() {
		MvcTestResult result = update(1L, """
				{"title": " "}
				""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.error.message").asString().contains("title: ");
		then(scheduleService).should(never()).update(any(), any(), any());
	}

	@Test
	void updateUnknownScheduleReturnsNotFound() {
		given(scheduleService.update(any(), any(), any()))
				.willThrow(new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

		MvcTestResult result = update(1L, """
				{"title": "마트"}
				""");

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("SCHEDULE_NOT_FOUND");
	}

	@Test
	void deleteReturnsOkWithoutData() {
		MvcTestResult result = delete(1L);

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.data").isNull();
		then(scheduleService).should().delete(5L, 1L);
	}

	@Test
	void deleteOfOtherFamilyReturnsForbidden() {
		willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER)).given(scheduleService).delete(5L, 1L);

		MvcTestResult result = delete(1L);

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void deleteWithoutAccessTokenReturnsUnauthorized() {
		MvcTestResult result = mvc.delete().uri("/api/schedules/5").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		then(scheduleService).should(never()).delete(any(), any());
	}

	private MvcTestResult create(Long userId, String body) {
		return mvc.post().uri("/api/families/3/schedules")
				.header("Authorization", bearer(userId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult update(Long userId, String body) {
		return mvc.patch().uri("/api/schedules/5")
				.header("Authorization", bearer(userId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.exchange();
	}

	private MvcTestResult delete(Long userId) {
		return mvc.delete().uri("/api/schedules/5")
				.header("Authorization", bearer(userId))
				.exchange();
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId, "USER");
	}

}
