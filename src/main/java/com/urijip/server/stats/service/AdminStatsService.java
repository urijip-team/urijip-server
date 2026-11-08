package com.urijip.server.stats.service;

import java.time.LocalDate;

import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.inquiry.service.InquiryService;
import com.urijip.server.member.service.UserService;
import com.urijip.server.report.service.ReportService;
import com.urijip.server.stats.dto.request.AdminStatsRequest;
import com.urijip.server.stats.dto.response.AdminStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 통계는 자기 테이블이 없다. 숫자는 각 도메인 서비스에서 받아 모은다.
@Service
@RequiredArgsConstructor
public class AdminStatsService {

	private final UserService userService;
	private final FamilyService familyService;
	private final ReportService reportService;
	private final InquiryService inquiryService;

	@Transactional(readOnly = true)
	public AdminStatsResponse getStats(AdminStatsRequest request) {
		AdminStatsRequest.Period period = request.resolve(LocalDate.now(SeoulTime.ZONE));
		return AdminStatsResponse.of(userService.countUsers(), familyService.countFamilies(),
				reportService.countPending(), inquiryService.countUnanswered(), period.from(), period.to(),
				userService.countSignupsByDay(period.from(), period.to()));
	}

}
