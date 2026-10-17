package com.urijip.server.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.photo.entity.Album;
import com.urijip.server.photo.entity.Photo;
import com.urijip.server.photo.repository.AlbumRepository;
import com.urijip.server.photo.repository.PhotoRepository;
import com.urijip.server.report.dto.request.AdminReportSearchRequest;
import com.urijip.server.report.dto.response.AdminReportResponse;
import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;
import com.urijip.server.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

// 사진은 S3를 쓰지 않도록 행만 직접 만든다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminReportServiceTest {

	@Autowired
	private AdminReportService adminReportService;

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AlbumRepository albumRepository;

	@Autowired
	private PhotoRepository photoRepository;

	private User admin;

	private User mom;

	private User troll;

	private Photo photo;

	@BeforeEach
	void setUp() {
		admin = saveUser("admin@example.com", "관리자");
		ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
		mom = saveUser("mom@example.com", "김엄마");
		troll = saveUser("troll@example.com", "악플러");
		FamilyResponse family = familyService.create(troll.getId(), new FamilyCreateRequest("악플가족"));
		photo = savePhoto(family.id());
	}

	@Test
	void searchReturnsReportsNewestFirstWithReporter() {
		Report userReport = reportRepository.save(new Report(mom, ReportTargetType.USER, troll.getId(), "욕설"));
		Report photoReport = reportRepository.save(new Report(mom, ReportTargetType.PHOTO, photo.getId(), "부적절"));

		PageResponse<AdminReportResponse> response = adminReportService.search(
				new AdminReportSearchRequest(null, null, null));

		assertThat(response.content()).extracting(AdminReportResponse::id)
				.containsExactly(photoReport.getId(), userReport.getId());
		AdminReportResponse first = response.content().getFirst();
		assertThat(first.targetType()).isEqualTo(ReportTargetType.PHOTO);
		assertThat(first.targetId()).isEqualTo(photo.getId());
		assertThat(first.reason()).isEqualTo("부적절");
		assertThat(first.status()).isEqualTo(ReportStatus.PENDING);
		assertThat(first.reporter()).isEqualTo(new AdminReportResponse.Reporter(mom.getId(), "mom@example.com", "김엄마"));
		assertThat(first.handledBy()).isNull();
		assertThat(first.createdAt().getOffset().getId()).isEqualTo("+09:00");
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isEqualTo(2);
	}

	@Test
	void searchFiltersByStatus() {
		reportRepository.save(new Report(mom, ReportTargetType.USER, troll.getId(), "욕설"));
		Report rejected = reportRepository.save(new Report(mom, ReportTargetType.PHOTO, photo.getId(), "부적절"));
		ReflectionTestUtils.setField(rejected, "status", ReportStatus.REJECTED);

		assertThat(adminReportService.search(new AdminReportSearchRequest(ReportStatus.REJECTED, 0, 10)).content())
				.extracting(AdminReportResponse::id)
				.containsExactly(rejected.getId());
	}

	private Photo savePhoto(Long familyId) {
		Album album = albumRepository.save(Album.builder()
				.family(familyService.getFamily(familyId))
				.title("여름")
				.createdBy(troll)
				.build());
		return photoRepository.save(Photo.builder()
				.album(album)
				.uploader(troll)
				.imageKey("bad.jpg")
				.thumbnailKey("bad_thumb.jpg")
				.build());
	}

	private void assertError(Runnable action, ErrorCode errorCode) {
		assertThatThrownBy(action::run)
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(errorCode);
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
