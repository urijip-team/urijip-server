package com.urijip.server.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notice.dto.request.AdminNoticeCreateRequest;
import com.urijip.server.notice.dto.request.AdminNoticeUpdateRequest;
import com.urijip.server.notice.dto.response.AdminNoticeResponse;
import com.urijip.server.notice.entity.Notice;
import com.urijip.server.notice.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminNoticeServiceTest {

	@Autowired
	private AdminNoticeService adminNoticeService;

	@Autowired
	private NoticeRepository noticeRepository;

	@Autowired
	private UserRepository userRepository;

	private User admin;

	@BeforeEach
	void setUp() {
		admin = userRepository.save(User.builder()
				.email("admin@example.com")
				.password("encoded-password")
				.name("관리자")
				.build());
		ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
	}

	@Test
	void createSavesNoticeWithAuthor() {
		AdminNoticeResponse response = adminNoticeService.create(admin.getId(),
				new AdminNoticeCreateRequest("서버 점검 안내", "내일 새벽 2시에 점검합니다."));

		assertThat(response.title()).isEqualTo("서버 점검 안내");
		assertThat(response.content()).isEqualTo("내일 새벽 2시에 점검합니다.");
		assertThat(response.isPushed()).isFalse();
		assertThat(response.createdBy()).isEqualTo(new AdminNoticeResponse.CreatedBy(admin.getId(), "관리자"));
		Notice saved = noticeRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getCreatedBy().getId()).isEqualTo(admin.getId());
	}

	@Test
	void createByUnknownAdminFails() {
		assertThatThrownBy(() -> adminNoticeService.create(-1L, new AdminNoticeCreateRequest("제목", "내용")))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
		assertThat(noticeRepository.count()).isZero();
	}

	@Test
	void updateChangesOnlySentFields() {
		Notice notice = noticeRepository.save(new Notice(admin, "서버 점검 안내", "내일 새벽 2시에 점검합니다."));

		AdminNoticeResponse response = adminNoticeService.update(notice.getId(),
				new AdminNoticeUpdateRequest(null, "내일 새벽 3시로 바뀌었습니다."));

		assertThat(response.title()).isEqualTo("서버 점검 안내");
		assertThat(response.content()).isEqualTo("내일 새벽 3시로 바뀌었습니다.");
		assertThat(notice.getContent()).isEqualTo("내일 새벽 3시로 바뀌었습니다.");
	}

	@Test
	void updateUnknownNoticeFails() {
		assertThatThrownBy(() -> adminNoticeService.update(-1L, new AdminNoticeUpdateRequest("제목", null)))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND));
	}

	@Test
	void deleteRemovesOnlyThatNotice() {
		Notice notice = noticeRepository.save(new Notice(admin, "지울 공지", "내용"));
		Notice other = noticeRepository.save(new Notice(admin, "남길 공지", "내용"));

		adminNoticeService.delete(notice.getId());

		assertThat(noticeRepository.findById(notice.getId())).isEmpty();
		assertThat(noticeRepository.findById(other.getId())).isPresent();
	}

	@Test
	void deleteUnknownNoticeFails() {
		assertThatThrownBy(() -> adminNoticeService.delete(-1L))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOTICE_NOT_FOUND));
	}

}
