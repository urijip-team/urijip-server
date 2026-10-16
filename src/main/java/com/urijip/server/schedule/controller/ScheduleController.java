package com.urijip.server.schedule.controller;

import java.time.YearMonth;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.schedule.dto.request.ScheduleCreateRequest;
import com.urijip.server.schedule.dto.request.ScheduleUpdateRequest;
import com.urijip.server.schedule.dto.response.MonthlyScheduleResponse;
import com.urijip.server.schedule.dto.response.ScheduleResponse;
import com.urijip.server.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

	private final ScheduleService scheduleService;

	@GetMapping("/api/families/{familyId}/schedules")
	public ApiResponse<MonthlyScheduleResponse> getMonthly(@AuthenticationPrincipal Long userId,
			@PathVariable Long familyId, @RequestParam YearMonth month) {
		return ApiResponse.success(scheduleService.getMonthly(familyId, userId, month));
	}

	@PostMapping("/api/families/{familyId}/schedules")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ScheduleResponse> create(@AuthenticationPrincipal Long userId, @PathVariable Long familyId,
			@Valid @RequestBody ScheduleCreateRequest request) {
		return ApiResponse.success(scheduleService.create(familyId, userId, request));
	}

	@PatchMapping("/api/schedules/{scheduleId}")
	public ApiResponse<ScheduleResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long scheduleId,
			@Valid @RequestBody ScheduleUpdateRequest request) {
		return ApiResponse.success(scheduleService.update(scheduleId, userId, request));
	}

	@DeleteMapping("/api/schedules/{scheduleId}")
	public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long scheduleId) {
		scheduleService.delete(scheduleId, userId);
		return ApiResponse.success(null);
	}

}
