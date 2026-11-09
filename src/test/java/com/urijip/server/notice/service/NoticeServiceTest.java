package com.urijip.server.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notice.dto.request.AdminNoticeUpdateRequest;
import com.urijip.server.notice.dto.request.NoticeSearchRequest;
import com.urijip.server.notice.dto.response.NoticeResponse;
import com.urijip.server.notice.entity.Notice;
import com.urijip.server.notice.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class NoticeServiceTest {

	@Autowired
	private NoticeService noticeService;

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
	}

	@Test
	void getNoticesReturnsNewestFirst() {
		Notice first = noticeRepository.save(new Notice(admin, "첫 공지", "내용"));
		Notice second = noticeRepository.save(new Notice(admin, "둘째 공지", "내용"));

		PageResponse<NoticeResponse> response = noticeService.getNotices(new NoticeSearchRequest(null, null));

		assertThat(response.content()).extracting(NoticeResponse::id).containsExactly(second.getId(), first.getId());
		assertThat(response.content().getFirst().createdAt().getOffset().getId()).isEqualTo("+09:00");
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isEqualTo(2);
	}

	@Test
	void getNoticesAppliesPaging() {
		noticeRepository.save(new Notice(admin, "첫 공지", "내용"));
		Notice second = noticeRepository.save(new Notice(admin, "둘째 공지", "내용"));

		PageResponse<NoticeResponse> response = noticeService.getNotices(new NoticeSearchRequest(0, 1));

		assertThat(response.content()).extracting(NoticeResponse::id).containsExactly(second.getId());
		assertThat(response.totalPages()).isEqualTo(2);
	}

	@Test
	void updatedNoticeKeepsItsPlace() {
		Notice first = noticeRepository.save(new Notice(admin, "첫 공지", "내용"));
		Notice second = noticeRepository.save(new Notice(admin, "둘째 공지", "내용"));

		adminNoticeService.update(first.getId(), admin.getId(), new AdminNoticeUpdateRequest("고친 첫 공지", null, null));

		assertThat(noticeService.getNotices(new NoticeSearchRequest(null, null)).content())
				.extracting(NoticeResponse::id, NoticeResponse::title)
				.containsExactly(tuple(second.getId(), "둘째 공지"), tuple(first.getId(), "고친 첫 공지"));
	}

	@Test
	void deletedNoticeIsHidden() {
		Notice kept = noticeRepository.save(new Notice(admin, "남길 공지", "내용"));
		Notice deleted = noticeRepository.save(new Notice(admin, "지울 공지", "내용"));

		adminNoticeService.delete(deleted.getId(), admin.getId());

		assertThat(noticeService.getNotices(new NoticeSearchRequest(null, null)).content())
				.extracting(NoticeResponse::id).containsExactly(kept.getId());
	}

}
