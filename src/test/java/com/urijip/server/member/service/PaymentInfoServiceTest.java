package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.dto.request.PaymentInfoRequest;
import com.urijip.server.member.dto.response.AccountNumberResponse;
import com.urijip.server.member.dto.response.PaymentInfoResponse;
import com.urijip.server.member.entity.PaymentInfo;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.PaymentInfoRepository;
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
class PaymentInfoServiceTest {

	private static final PaymentInfoRequest MOM_ACCOUNT = new PaymentInfoRequest("국민은행", "12345678905678", "김엄마",
			"https://toss.me/example");

	@Autowired
	private PaymentInfoService paymentInfoService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private PaymentInfoRepository paymentInfoRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User dad;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		dad = saveUser("dad@example.com", "김아빠");
	}

	@Test
	void registerReturnsMaskedPaymentInfo() {
		PaymentInfoResponse response = paymentInfoService.register(mom.getId(), MOM_ACCOUNT);

		assertThat(response).isEqualTo(new PaymentInfoResponse("국민은행", "****-5678", "김엄마",
				"https://toss.me/example"));
	}

	@Test
	void registerStoresEncryptedAccountNumberAndLast4() {
		paymentInfoService.register(mom.getId(), MOM_ACCOUNT);

		PaymentInfo saved = paymentInfoRepository.findById(mom.getId()).orElseThrow();
		assertThat(saved.getAccountNumberEnc()).isNotBlank().doesNotContain("12345678905678");
		assertThat(saved.getAccountLast4()).isEqualTo("5678");
	}

	@Test
	void registerAgainReplacesPaymentInfo() {
		paymentInfoService.register(mom.getId(), MOM_ACCOUNT);

		PaymentInfoResponse response = paymentInfoService.register(mom.getId(),
				new PaymentInfoRequest("신한은행", "1100223344", "김영희", null));

		assertThat(response).isEqualTo(new PaymentInfoResponse("신한은행", "****-3344", "김영희", null));
		assertThat(paymentInfoRepository.count()).isEqualTo(1);
		assertThat(paymentInfoService.getAccountNumber(mom.getId(), mom.getId()).accountNumber())
				.isEqualTo("1100223344");
	}

	@Test
	void registerRejectsUnknownUser() {
		assertThatThrownBy(() -> paymentInfoService.register(999_999L, MOM_ACCOUNT))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void getPaymentInfoReturnsMaskedInfoToFamilyMember() {
		joinSameFamily();
		paymentInfoService.register(mom.getId(), MOM_ACCOUNT);

		PaymentInfoResponse response = paymentInfoService.getPaymentInfo(dad.getId(), mom.getId());

		assertThat(response).isEqualTo(new PaymentInfoResponse("국민은행", "****-5678", "김엄마",
				"https://toss.me/example"));
	}

	@Test
	void getPaymentInfoReturnsOwnInfoWithoutFamily() {
		paymentInfoService.register(mom.getId(), MOM_ACCOUNT);

		assertThat(paymentInfoService.getPaymentInfo(mom.getId(), mom.getId()).maskedAccount())
				.isEqualTo("****-5678");
	}

	@Test
	void getPaymentInfoRejectsUserOutsideFamily() {
		familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		paymentInfoService.register(mom.getId(), MOM_ACCOUNT);

		assertThatThrownBy(() -> paymentInfoService.getPaymentInfo(dad.getId(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getPaymentInfoRejectsMemberWhoLeftFamily() {
		FamilyResponse family = joinSameFamily();
		paymentInfoService.register(mom.getId(), MOM_ACCOUNT);
		familyService.removeMember(family.id(), dad.getId(), dad.getId());

		assertThatThrownBy(() -> paymentInfoService.getPaymentInfo(dad.getId(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getPaymentInfoOfMemberWithoutPaymentInfoIsNotFound() {
		joinSameFamily();

		assertThatThrownBy(() -> paymentInfoService.getPaymentInfo(dad.getId(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PAYMENT_INFO_NOT_FOUND);
	}

	@Test
	void getAccountNumberReturnsDecryptedNumberToFamilyMember() {
		joinSameFamily();
		paymentInfoService.register(mom.getId(), MOM_ACCOUNT);

		AccountNumberResponse response = paymentInfoService.getAccountNumber(dad.getId(), mom.getId());

		assertThat(response).isEqualTo(new AccountNumberResponse("12345678905678"));
	}

	@Test
	void getAccountNumberRejectsUserOutsideFamily() {
		familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		paymentInfoService.register(mom.getId(), MOM_ACCOUNT);

		assertThatThrownBy(() -> paymentInfoService.getAccountNumber(dad.getId(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getAccountNumberWithoutPaymentInfoIsNotFound() {
		assertThatThrownBy(() -> paymentInfoService.getAccountNumber(mom.getId(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.PAYMENT_INFO_NOT_FOUND);
	}

	private FamilyResponse joinSameFamily() {
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		familyService.join(dad.getId(), new FamilyJoinRequest(family.inviteCode()));
		return family;
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
