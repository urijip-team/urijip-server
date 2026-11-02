package com.urijip.server.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.inquiry.dto.request.InquiryCreateRequest;
import com.urijip.server.inquiry.dto.request.InquirySearchRequest;
import com.urijip.server.inquiry.dto.response.InquiryResponse;
import com.urijip.server.inquiry.entity.Inquiry;
import com.urijip.server.inquiry.repository.InquiryRepository;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class InquiryServiceTest {

	@Autowired
	private InquiryService inquiryService;

	@Autowired
	private InquiryRepository inquiryRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
	}

	@Test
	void createSavesInquiryWithoutAnswer() {
		InquiryResponse response = inquiryService.create(mom.getId(),
				new InquiryCreateRequest("로그인이 안 돼요", "비밀번호가 맞는데 실패합니다."));

		assertThat(response.title()).isEqualTo("로그인이 안 돼요");
		assertThat(response.content()).isEqualTo("비밀번호가 맞는데 실패합니다.");
		assertThat(response.answer()).isNull();
		assertThat(response.answeredAt()).isNull();
		Inquiry saved = inquiryRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getUser().getId()).isEqualTo(mom.getId());
	}

	@Test
	void createByUnknownUserFails() {
		assertThatThrownBy(() -> inquiryService.create(-1L, new InquiryCreateRequest("제목", "내용")))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
	}

	@Test
	void getMyInquiriesReturnsOnlyMineNewestFirst() {
		User dad = saveUser("dad@example.com", "김아빠");
		InquiryResponse first = inquiryService.create(mom.getId(), new InquiryCreateRequest("첫 문의", "내용"));
		inquiryService.create(dad.getId(), new InquiryCreateRequest("아빠 문의", "내용"));
		InquiryResponse last = inquiryService.create(mom.getId(), new InquiryCreateRequest("둘째 문의", "내용"));

		PageResponse<InquiryResponse> response = inquiryService.getMyInquiries(mom.getId(),
				new InquirySearchRequest(null, null));

		assertThat(response.content()).extracting(InquiryResponse::id).containsExactly(last.id(), first.id());
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isEqualTo(2);
	}

	@Test
	void getMyInquiriesAppliesPaging() {
		inquiryService.create(mom.getId(), new InquiryCreateRequest("첫 문의", "내용"));
		InquiryResponse last = inquiryService.create(mom.getId(), new InquiryCreateRequest("둘째 문의", "내용"));

		PageResponse<InquiryResponse> response = inquiryService.getMyInquiries(mom.getId(),
				new InquirySearchRequest(0, 1));

		assertThat(response.content()).extracting(InquiryResponse::id).containsExactly(last.id());
		assertThat(response.totalPages()).isEqualTo(2);
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
