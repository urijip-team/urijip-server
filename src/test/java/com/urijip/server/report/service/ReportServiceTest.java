package com.urijip.server.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.photo.entity.Album;
import com.urijip.server.photo.entity.Photo;
import com.urijip.server.photo.repository.AlbumRepository;
import com.urijip.server.photo.repository.PhotoRepository;
import com.urijip.server.report.dto.request.ReportCreateRequest;
import com.urijip.server.report.dto.response.ReportResponse;
import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;
import com.urijip.server.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

// 사진 신고는 S3를 쓰지 않으므로 사진 행만 직접 만든다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ReportServiceTest {

	@Autowired
	private ReportService reportService;

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

	private User mom;

	private User stranger;

	private Photo photo;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		stranger = saveUser("stranger@example.com", "남");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		photo = savePhoto(family.id());
	}

	@Test
	void reportUserSavesPendingReport() {
		ReportResponse response = reportService.create(mom.getId(),
				new ReportCreateRequest(ReportTargetType.USER, stranger.getId(), "욕설"));

		assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
		Report saved = reportRepository.findById(response.reportId()).orElseThrow();
		assertThat(saved.getReporter().getId()).isEqualTo(mom.getId());
		assertThat(saved.getTargetType()).isEqualTo(ReportTargetType.USER);
		assertThat(saved.getTargetId()).isEqualTo(stranger.getId());
		assertThat(saved.getReason()).isEqualTo("욕설");
	}

	@Test
	void reportUnknownUserIsNotFound() {
		assertError(() -> reportService.create(mom.getId(),
				new ReportCreateRequest(ReportTargetType.USER, 999_999L, "욕설")), ErrorCode.USER_NOT_FOUND);
		assertThat(reportRepository.count()).isZero();
	}

	@Test
	void reportingSelfIsRejected() {
		assertError(() -> reportService.create(mom.getId(),
				new ReportCreateRequest(ReportTargetType.USER, mom.getId(), "욕설")), ErrorCode.CANNOT_REPORT_SELF);
		assertThat(reportRepository.count()).isZero();
	}

	@Test
	void reportPhotoOfOwnFamilySavesReport() {
		ReportResponse response = reportService.create(mom.getId(),
				new ReportCreateRequest(ReportTargetType.PHOTO, photo.getId(), "부적절한 사진"));

		Report saved = reportRepository.findById(response.reportId()).orElseThrow();
		assertThat(saved.getTargetType()).isEqualTo(ReportTargetType.PHOTO);
		assertThat(saved.getTargetId()).isEqualTo(photo.getId());
	}

	@Test
	void reportPhotoOfOtherFamilyIsForbidden() {
		assertError(() -> reportService.create(stranger.getId(),
				new ReportCreateRequest(ReportTargetType.PHOTO, photo.getId(), "부적절한 사진")),
				ErrorCode.FAMILY_NOT_MEMBER);
		assertError(() -> reportService.create(mom.getId(),
				new ReportCreateRequest(ReportTargetType.PHOTO, 999_999L, "부적절한 사진")),
				ErrorCode.PHOTO_NOT_FOUND);
		assertThat(reportRepository.count()).isZero();
	}

	@Test
	void reportingSameTargetAgainWhilePendingIsRejected() {
		ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.PHOTO, photo.getId(), "부적절한 사진");
		reportService.create(mom.getId(), request);

		assertError(() -> reportService.create(mom.getId(), request), ErrorCode.ALREADY_REPORTED);
		assertThat(reportRepository.count()).isEqualTo(1);
	}

	@Test
	void otherReporterCanReportSameTarget() {
		reportService.create(mom.getId(), new ReportCreateRequest(ReportTargetType.USER, stranger.getId(), "욕설"));
		User dad = saveUser("dad@example.com", "김아빠");

		reportService.create(dad.getId(), new ReportCreateRequest(ReportTargetType.USER, stranger.getId(), "욕설"));

		assertThat(reportRepository.count()).isEqualTo(2);
	}

	private Photo savePhoto(Long familyId) {
		Album album = albumRepository.save(Album.builder()
				.family(familyService.getFamily(familyId))
				.title("여름")
				.createdBy(mom)
				.build());
		return photoRepository.save(Photo.builder()
				.album(album)
				.uploader(mom)
				.imageKey("beach.jpg")
				.thumbnailKey("beach_thumb.jpg")
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
