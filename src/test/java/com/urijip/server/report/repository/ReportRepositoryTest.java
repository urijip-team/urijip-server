package com.urijip.server.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class ReportRepositoryTest {

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	@BeforeEach
	void setUp() {
		mom = userRepository.saveAndFlush(User.builder()
				.email("mom@example.com")
				.password("encoded-password")
				.name("김엄마")
				.build());
	}

	@Test
	void newReportIsPendingWithoutHandler() {
		Report report = reportRepository.saveAndFlush(new Report(mom, ReportTargetType.PHOTO, 42L, "부적절한 사진"));

		Report found = reportRepository.findById(report.getId()).orElseThrow();
		assertThat(found.getReporter().getId()).isEqualTo(mom.getId());
		assertThat(found.getTargetType()).isEqualTo(ReportTargetType.PHOTO);
		assertThat(found.getTargetId()).isEqualTo(42L);
		assertThat(found.getReason()).isEqualTo("부적절한 사진");
		assertThat(found.getStatus()).isEqualTo(ReportStatus.PENDING);
		assertThat(found.getHandledBy()).isNull();
		assertThat(found.getCreatedAt()).isNotNull();
	}

	@Test
	void existsByReporterAndTargetMatchesOnlySameReporterTargetAndStatus() {
		User dad = userRepository.saveAndFlush(User.builder()
				.email("dad@example.com")
				.password("encoded-password")
				.name("김아빠")
				.build());
		reportRepository.saveAndFlush(new Report(mom, ReportTargetType.PHOTO, 42L, "부적절한 사진"));

		assertThat(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(mom.getId(),
				ReportTargetType.PHOTO, 42L, ReportStatus.PENDING)).isTrue();
		assertThat(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(dad.getId(),
				ReportTargetType.PHOTO, 42L, ReportStatus.PENDING)).isFalse();
		assertThat(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(mom.getId(),
				ReportTargetType.USER, 42L, ReportStatus.PENDING)).isFalse();
		assertThat(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(mom.getId(),
				ReportTargetType.PHOTO, 43L, ReportStatus.PENDING)).isFalse();
		assertThat(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(mom.getId(),
				ReportTargetType.PHOTO, 42L, ReportStatus.DONE)).isFalse();
	}

	@Test
	void searchFiltersByStatusNewestFirst() {
		Report first = reportRepository.save(new Report(mom, ReportTargetType.PHOTO, 1L, "첫 신고"));
		Report done = reportRepository.save(new Report(mom, ReportTargetType.PHOTO, 2L, "처리된 신고"));
		ReflectionTestUtils.setField(done, "status", ReportStatus.DONE);
		Report last = reportRepository.saveAndFlush(new Report(mom, ReportTargetType.USER, 3L, "마지막 신고"));
		PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));

		assertThat(reportRepository.search(ReportStatus.PENDING, pageable).getContent())
				.extracting(Report::getId)
				.containsExactly(last.getId(), first.getId());
		assertThat(reportRepository.search(ReportStatus.DONE, pageable).getContent())
				.extracting(Report::getId)
				.containsExactly(done.getId());
		Page<Report> all = reportRepository.search(null, pageable);
		assertThat(all.getContent()).extracting(Report::getId)
				.containsExactly(last.getId(), done.getId(), first.getId());
		assertThat(all.getTotalElements()).isEqualTo(3);
	}

}
