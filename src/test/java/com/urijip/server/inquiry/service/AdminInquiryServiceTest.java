package com.urijip.server.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.inquiry.dto.request.AdminInquirySearchRequest;
import com.urijip.server.inquiry.dto.response.AdminInquiryResponse;
import com.urijip.server.inquiry.entity.Inquiry;
import com.urijip.server.inquiry.repository.InquiryRepository;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
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
class AdminInquiryServiceTest {

	@Autowired
	private AdminInquiryService adminInquiryService;

	@Autowired
	private InquiryRepository inquiryRepository;

	@Autowired
	private UserRepository userRepository;

	private User admin;

	private User mom;

	@BeforeEach
	void setUp() {
		admin = saveUser("admin@example.com", "관리자");
		ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
		mom = saveUser("mom@example.com", "김엄마");
	}

	@Test
	void searchPutsUnansweredFirstWithAuthor() {
		Inquiry answered = saveAnswered("답변 완료");
		Inquiry unanswered = inquiryRepository.save(new Inquiry(mom, "미답변", "내용"));

		PageResponse<AdminInquiryResponse> response = adminInquiryService.search(
				new AdminInquirySearchRequest(null, null, null));

		assertThat(response.content()).extracting(AdminInquiryResponse::id)
				.containsExactly(unanswered.getId(), answered.getId());
		AdminInquiryResponse first = response.content().getFirst();
		assertThat(first.user()).isEqualTo(new AdminInquiryResponse.Author(mom.getId(), "mom@example.com", "김엄마"));
		assertThat(first.title()).isEqualTo("미답변");
		assertThat(first.answer()).isNull();
		assertThat(first.answeredBy()).isNull();
		assertThat(first.answeredAt()).isNull();
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isEqualTo(2);
	}

	@Test
	void searchFiltersUnanswered() {
		saveAnswered("답변 완료");
		Inquiry unanswered = inquiryRepository.save(new Inquiry(mom, "미답변", "내용"));

		PageResponse<AdminInquiryResponse> response = adminInquiryService.search(
				new AdminInquirySearchRequest(false, null, null));

		assertThat(response.content()).extracting(AdminInquiryResponse::id).containsExactly(unanswered.getId());
	}

	@Test
	void searchShowsWhoAnswered() {
		saveAnswered("답변 완료");

		AdminInquiryResponse found = adminInquiryService.search(new AdminInquirySearchRequest(true, null, null))
				.content().getFirst();

		assertThat(found.answer()).isEqualTo("답변입니다");
		assertThat(found.answeredBy()).isEqualTo(new AdminInquiryResponse.AnsweredBy(admin.getId(), "관리자"));
		assertThat(found.answeredAt().getOffset().getId()).isEqualTo("+09:00");
	}

	private Inquiry saveAnswered(String title) {
		Inquiry inquiry = new Inquiry(mom, title, "내용");
		ReflectionTestUtils.setField(inquiry, "answer", "답변입니다");
		ReflectionTestUtils.setField(inquiry, "answeredBy", admin);
		ReflectionTestUtils.setField(inquiry, "answeredAt", LocalDateTime.of(2026, 11, 2, 10, 0));
		return inquiryRepository.save(inquiry);
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
