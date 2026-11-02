package com.urijip.server.inquiry.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.inquiry.entity.Inquiry;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class InquiryRepositoryTest {

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
	void newInquiryHasNoAnswer() {
		Inquiry inquiry = inquiryRepository.saveAndFlush(new Inquiry(mom, "로그인이 안 돼요", "비밀번호가 맞는데 실패합니다."));

		Inquiry found = inquiryRepository.findById(inquiry.getId()).orElseThrow();
		assertThat(found.getUser().getId()).isEqualTo(mom.getId());
		assertThat(found.getTitle()).isEqualTo("로그인이 안 돼요");
		assertThat(found.getContent()).isEqualTo("비밀번호가 맞는데 실패합니다.");
		assertThat(found.getAnswer()).isNull();
		assertThat(found.getAnsweredBy()).isNull();
		assertThat(found.getAnsweredAt()).isNull();
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
