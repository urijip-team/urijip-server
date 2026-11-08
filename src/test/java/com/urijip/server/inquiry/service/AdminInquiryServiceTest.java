package com.urijip.server.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.push.PushType;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.inquiry.dto.request.AdminInquiryAnswerRequest;
import com.urijip.server.inquiry.dto.request.AdminInquirySearchRequest;
import com.urijip.server.inquiry.dto.request.InquiryCreateRequest;
import com.urijip.server.inquiry.dto.request.InquirySearchRequest;
import com.urijip.server.inquiry.dto.response.AdminInquiryResponse;
import com.urijip.server.inquiry.dto.response.InquiryResponse;
import com.urijip.server.inquiry.entity.Inquiry;
import com.urijip.server.inquiry.repository.InquiryRepository;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notification.service.PushRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RecordApplicationEvents
class AdminInquiryServiceTest {

	@Autowired
	private ApplicationEvents events;

	@Autowired
	private AdminInquiryService adminInquiryService;

	@Autowired
	private InquiryService inquiryService;

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

	@Test
	void answerSavesAnswerAndAdmin() {
		Inquiry inquiry = inquiryRepository.save(new Inquiry(mom, "미답변", "내용"));

		AdminInquiryResponse response = adminInquiryService.answer(inquiry.getId(), admin.getId(),
				new AdminInquiryAnswerRequest("다시 시도해 주세요."));

		assertThat(response.answer()).isEqualTo("다시 시도해 주세요.");
		assertThat(response.answeredBy()).isEqualTo(new AdminInquiryResponse.AnsweredBy(admin.getId(), "관리자"));
		assertThat(response.answeredAt().getOffset().getId()).isEqualTo("+09:00");
		assertThat(inquiry.isAnswered()).isTrue();
	}

	@Test
	void answerPushesToAuthor() {
		Inquiry inquiry = inquiryRepository.save(new Inquiry(mom, "사진이 안 올라가요", "내용"));

		adminInquiryService.answer(inquiry.getId(), admin.getId(), new AdminInquiryAnswerRequest("다시 시도해 주세요."));

		assertThat(events.stream(PushRequestedEvent.class)).singleElement().satisfies(event -> {
			assertThat(event.userIds()).isEqualTo(Set.of(mom.getId()));
			assertThat(event.message().type()).isEqualTo(PushType.INQUIRY);
			assertThat(event.message().title()).isEqualTo("문의 답변");
			assertThat(event.message().body()).isEqualTo("'사진이 안 올라가요' 문의에 답변이 등록됐어요.");
			assertThat(event.message().data()).isEqualTo(Map.of("inquiryId", inquiry.getId().toString()));
		});
	}

	@Test
	void answeredInquiryIsVisibleToAuthor() {
		InquiryResponse created = inquiryService.create(mom.getId(), new InquiryCreateRequest("미답변", "내용"));

		adminInquiryService.answer(created.id(), admin.getId(), new AdminInquiryAnswerRequest("다시 시도해 주세요."));

		InquiryResponse mine = inquiryService.getMyInquiries(mom.getId(), new InquirySearchRequest(null, null))
				.content().getFirst();
		assertThat(mine.answer()).isEqualTo("다시 시도해 주세요.");
		assertThat(mine.answeredAt()).isNotNull();
	}

	@Test
	void answerTwiceFails() {
		Inquiry inquiry = inquiryRepository.save(new Inquiry(mom, "미답변", "내용"));
		adminInquiryService.answer(inquiry.getId(), admin.getId(), new AdminInquiryAnswerRequest("첫 답변"));

		assertThatThrownBy(() -> adminInquiryService.answer(inquiry.getId(), admin.getId(),
				new AdminInquiryAnswerRequest("둘째 답변")))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_ALREADY_ANSWERED));
		assertThat(inquiry.getAnswer()).isEqualTo("첫 답변");
		assertThat(events.stream(PushRequestedEvent.class)).hasSize(1);
	}

	@Test
	void answerUnknownInquiryFails() {
		assertThatThrownBy(() -> adminInquiryService.answer(-1L, admin.getId(), new AdminInquiryAnswerRequest("답변")))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_FOUND));
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
