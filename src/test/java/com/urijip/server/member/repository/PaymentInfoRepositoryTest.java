package com.urijip.server.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.PaymentInfo;
import com.urijip.server.member.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class PaymentInfoRepositoryTest {

	@Autowired
	private PaymentInfoRepository paymentInfoRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
	}

	@Test
	void savedPaymentInfoUsesUserIdAsId() {
		PaymentInfo saved = paymentInfoRepository.saveAndFlush(paymentInfo(mom));

		assertThat(saved.getUserId()).isEqualTo(mom.getId());
		assertThat(paymentInfoRepository.findById(mom.getId())).isPresent();
	}

	@Test
	void savedPaymentInfoKeepsAllFields() {
		paymentInfoRepository.saveAndFlush(paymentInfo(mom));

		PaymentInfo found = paymentInfoRepository.findById(mom.getId()).orElseThrow();

		assertThat(found.getBankName()).isEqualTo("국민은행");
		assertThat(found.getAccountNumberEnc()).isEqualTo("encrypted");
		assertThat(found.getAccountLast4()).isEqualTo("5678");
		assertThat(found.getAccountHolder()).isEqualTo("김엄마");
		assertThat(found.getTransferLink()).isEqualTo("https://toss.me/example");
	}

	@Test
	void transferLinkCanBeNull() {
		paymentInfoRepository.saveAndFlush(new PaymentInfo(mom, "국민은행", "encrypted", "5678", "김엄마", null));

		assertThat(paymentInfoRepository.findById(mom.getId()).orElseThrow().getTransferLink()).isNull();
	}

	@Test
	void deleteByIdDeletesOnlyThatUser() {
		paymentInfoRepository.saveAndFlush(paymentInfo(mom));
		User dad = saveUser("dad@example.com", "김아빠");
		paymentInfoRepository.saveAndFlush(paymentInfo(dad));

		paymentInfoRepository.deleteById(mom.getId());

		assertThat(paymentInfoRepository.findAll())
				.extracting(PaymentInfo::getUserId)
				.containsExactly(dad.getId());
	}

	private PaymentInfo paymentInfo(User user) {
		return new PaymentInfo(user, "국민은행", "encrypted", "5678", "김엄마", "https://toss.me/example");
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
