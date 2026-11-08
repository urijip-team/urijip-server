package com.urijip.server.stats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.inquiry.dto.request.InquiryCreateRequest;
import com.urijip.server.inquiry.service.InquiryService;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.report.dto.request.ReportCreateRequest;
import com.urijip.server.report.entity.ReportTargetType;
import com.urijip.server.report.service.ReportService;
import com.urijip.server.stats.dto.request.AdminStatsRequest;
import com.urijip.server.stats.dto.response.AdminStatsResponse;
import com.urijip.server.stats.dto.response.DailyCount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminStatsServiceTest {

	@Autowired
	private AdminStatsService adminStatsService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private ReportService reportService;

	@Autowired
	private InquiryService inquiryService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void cardsCountCurrentData() {
		AdminStatsResponse before = adminStatsService.getStats(new AdminStatsRequest(null, null));
		User mom = saveUser("mom@example.com");
		User dad = saveUser("dad@example.com");
		familyService.create(mom.getId(), new FamilyCreateRequest("우리집"));
		reportService.create(mom.getId(), new ReportCreateRequest(ReportTargetType.USER, dad.getId(), "욕설"));
		inquiryService.create(mom.getId(), new InquiryCreateRequest("문의", "내용"));

		AdminStatsResponse after = adminStatsService.getStats(new AdminStatsRequest(null, null));

		assertThat(after.totalUsers()).isEqualTo(before.totalUsers() + 2);
		assertThat(after.totalFamilies()).isEqualTo(before.totalFamilies() + 1);
		assertThat(after.pendingReports()).isEqualTo(before.pendingReports() + 1);
		assertThat(after.unansweredInquiries()).isEqualTo(before.unansweredInquiries() + 1);
	}

	@Test
	void defaultPeriodIsLastSevenDaysIncludingTodaysSignups() {
		LocalDate today = LocalDate.now(SeoulTime.ZONE);
		long todayBefore = adminStatsService.getStats(new AdminStatsRequest(null, null)).signups().getLast().count();
		saveUser("new@example.com");

		AdminStatsResponse response = adminStatsService.getStats(new AdminStatsRequest(null, null));

		assertThat(response.from()).isEqualTo(today.minusDays(6));
		assertThat(response.to()).isEqualTo(today);
		assertThat(response.signups()).hasSize(7);
		assertThat(response.signups().getLast()).isEqualTo(new DailyCount(today, todayBefore + 1));
	}

	@Test
	void signupsCoverRequestedPeriod() {
		signupAt("a@example.com", LocalDateTime.of(2020, 3, 1, 10, 0));
		signupAt("b@example.com", LocalDateTime.of(2020, 3, 3, 10, 0));
		signupAt("c@example.com", LocalDateTime.of(2020, 3, 3, 23, 0));

		AdminStatsResponse response = adminStatsService.getStats(
				new AdminStatsRequest(LocalDate.of(2020, 3, 1), LocalDate.of(2020, 3, 3)));

		assertThat(response.signups()).containsExactly(new DailyCount(LocalDate.of(2020, 3, 1), 1),
				new DailyCount(LocalDate.of(2020, 3, 2), 0), new DailyCount(LocalDate.of(2020, 3, 3), 2));
		assertThat(response.signupCount()).isEqualTo(3);
	}

	@Test
	void invalidPeriodFails() {
		assertThatThrownBy(() -> adminStatsService.getStats(
				new AdminStatsRequest(LocalDate.of(2020, 3, 3), LocalDate.of(2020, 3, 1))))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
	}

	private void signupAt(String email, LocalDateTime createdAt) {
		User user = saveUser(email);
		userRepository.flush();
		jdbcTemplate.update("update users set created_at = ? where id = ?", createdAt, user.getId());
	}

	private User saveUser(String email) {
		return userRepository.save(User.builder().email(email).password("encoded-password").name("회원").build());
	}

}
