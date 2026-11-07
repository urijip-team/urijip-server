package com.urijip.server.allowance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.allowance.dto.request.AllowanceCreateRequest;
import com.urijip.server.allowance.dto.response.AllowanceCreateResponse;
import com.urijip.server.allowance.dto.response.AllowanceDetailResponse;
import com.urijip.server.allowance.dto.response.AllowanceResponse;
import com.urijip.server.allowance.entity.AllowanceRequest;
import com.urijip.server.allowance.entity.AllowanceStatus;
import com.urijip.server.allowance.repository.AllowanceRequestRepository;
import com.urijip.server.chat.entity.MessageType;
import com.urijip.server.chat.repository.ChatMessageRepository;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.request.NicknameUpdateRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.dto.request.PaymentInfoRequest;
import com.urijip.server.member.dto.response.PaymentInfoResponse;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.service.PaymentInfoService;
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
class AllowanceServiceTest {

	@Autowired
	private AllowanceService allowanceService;

	@Autowired
	private AllowanceRequestRepository allowanceRequestRepository;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private PaymentInfoService paymentInfoService;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User kid;

	private User stranger;

	private Long familyId;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		kid = saveUser("kid@example.com", "김첫째");
		stranger = saveUser("stranger@example.com", "남");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("우리집"));
		familyService.join(kid.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyId = family.id();
	}

	@Test
	void createSavesRequestedAllowance() {
		AllowanceCreateResponse response = request(kid, mom, 30000, "문제집 사야 해요");

		assertThat(response.status()).isEqualTo(AllowanceStatus.REQUESTED);
		AllowanceRequest saved = allowanceRequestRepository.findById(response.requestId()).orElseThrow();
		assertThat(saved.getFamily().getId()).isEqualTo(familyId);
		assertThat(saved.getRequester().getId()).isEqualTo(kid.getId());
		assertThat(saved.getReceiver().getId()).isEqualTo(mom.getId());
		assertThat(saved.getAmount()).isEqualTo(30000);
		assertThat(saved.getReason()).isEqualTo("문제집 사야 해요");
	}

	@Test
	void createPostsSystemMessageWithNickname() {
		familyService.changeMyNickname(familyId, kid.getId(), new NicknameUpdateRequest("첫째"));

		request(kid, mom, 30000, "문제집 사야 해요");

		assertThat(chatMessageRepository.findAll()).singleElement().satisfies(message -> {
			assertThat(message.getType()).isEqualTo(MessageType.SYSTEM);
			assertThat(message.getFamily().getId()).isEqualTo(familyId);
			assertThat(message.getContent()).isEqualTo("첫째님이 용돈 30,000원을 요청했어요.");
		});
	}

	@Test
	void createPostsSystemMessageWithNameWhenNoNickname() {
		request(kid, mom, 1500000, null);

		assertThat(chatMessageRepository.findAll()).singleElement()
				.satisfies(message -> assertThat(message.getContent()).isEqualTo("김첫째님이 용돈 1,500,000원을 요청했어요."));
	}

	@Test
	void failedCreatePostsNoMessage() {
		assertThatThrownBy(() -> request(kid, kid, 5000, null)).isInstanceOf(BusinessException.class);

		assertThat(chatMessageRepository.count()).isZero();
	}

	@Test
	void createStoresBlankReasonAsNull() {
		AllowanceCreateResponse response = request(kid, mom, 5000, "  ");

		assertThat(allowanceRequestRepository.findById(response.requestId()).orElseThrow().getReason()).isNull();
	}

	@Test
	void createByNonMemberFails() {
		assertThatThrownBy(() -> request(stranger, mom, 5000, null))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FAMILY_NOT_MEMBER));
	}

	@Test
	void createToNonMemberFails() {
		assertThatThrownBy(() -> request(kid, stranger, 5000, null))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FAMILY_MEMBER_NOT_FOUND));
		assertThat(allowanceRequestRepository.count()).isZero();
	}

	@Test
	void createToSelfFails() {
		assertThatThrownBy(() -> request(kid, kid, 5000, null))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CANNOT_REQUEST_SELF));
	}

	@Test
	void getDetailIncludesRequesterPaymentInfo() {
		paymentInfoService.register(kid.getId(), new PaymentInfoRequest("국민은행", "12345678905678", "김첫째",
				"https://toss.me/kid"));
		Long requestId = request(kid, mom, 30000, "문제집 사야 해요").requestId();

		AllowanceDetailResponse detail = allowanceService.getDetail(requestId, mom.getId());

		assertThat(detail.id()).isEqualTo(requestId);
		assertThat(detail.familyId()).isEqualTo(familyId);
		assertThat(detail.requester()).isEqualTo(new AllowanceResponse.Person(kid.getId(), "김첫째"));
		assertThat(detail.receiver()).isEqualTo(new AllowanceResponse.Person(mom.getId(), "김엄마"));
		assertThat(detail.amount()).isEqualTo(30000);
		assertThat(detail.reason()).isEqualTo("문제집 사야 해요");
		assertThat(detail.status()).isEqualTo(AllowanceStatus.REQUESTED);
		assertThat(detail.createdAt().getOffset().getId()).isEqualTo("+09:00");
		assertThat(detail.completedAt()).isNull();
		assertThat(detail.requesterPaymentInfo()).isEqualTo(new PaymentInfoResponse("국민은행", "****-5678", "김첫째",
				"https://toss.me/kid"));
	}

	@Test
	void requesterCanSeeDetailWithoutPaymentInfo() {
		Long requestId = request(kid, mom, 30000, null).requestId();

		AllowanceDetailResponse detail = allowanceService.getDetail(requestId, kid.getId());

		assertThat(detail.requesterPaymentInfo()).isNull();
	}

	@Test
	void otherFamilyMemberCannotSeeDetail() {
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(familyService.getMyFamily(mom.getId()).inviteCode()));
		Long requestId = request(kid, mom, 30000, null).requestId();

		assertThatThrownBy(() -> allowanceService.getDetail(requestId, dad.getId()))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_ALLOWANCE_PARTICIPANT));
	}

	@Test
	void getUnknownDetailFails() {
		assertThatThrownBy(() -> allowanceService.getDetail(-1L, mom.getId()))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALLOWANCE_NOT_FOUND));
	}

	private AllowanceCreateResponse request(User requester, User receiver, int amount, String reason) {
		return allowanceService.create(requester.getId(),
				new AllowanceCreateRequest(familyId, receiver.getId(), amount, reason));
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
