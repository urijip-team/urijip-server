package com.urijip.server.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willReturn;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyRole;
import com.urijip.server.family.entity.FamilyStatus;
import com.urijip.server.family.repository.FamilyMemberRepository;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class FamilyServiceTest {

	@Autowired
	private FamilyService familyService;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private FamilyMemberRepository familyMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@MockitoSpyBean
	private InviteCodeGenerator inviteCodeGenerator;

	private User mom;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
	}

	@Test
	void createReturnsFamilyWithInviteCode() {
		FamilyResponse response = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThat(response.id()).isNotNull();
		assertThat(response.name()).isEqualTo("김씨네");
		assertThat(response.inviteCode()).matches("[A-HJ-NP-Z2-9]{6}");
	}

	@Test
	void createSavesActiveFamilyOwnedByCreator() {
		FamilyResponse response = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		Family saved = familyRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getOwner().getId()).isEqualTo(mom.getId());
		assertThat(saved.getStatus()).isEqualTo(FamilyStatus.ACTIVE);
		assertThat(saved.getInviteCode()).isEqualTo(response.inviteCode());
	}

	@Test
	void createRegistersCreatorAsOwnerMember() {
		FamilyResponse response = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThat(familyMemberRepository.findAll()).singleElement().satisfies(member -> {
			assertThat(member.getFamily().getId()).isEqualTo(response.id());
			assertThat(member.getUser().getId()).isEqualTo(mom.getId());
			assertThat(member.getRole()).isEqualTo(FamilyRole.OWNER);
		});
	}

	@Test
	void createRejectsUserWhoAlreadyHasFamily() {
		familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThatThrownBy(() -> familyService.create(mom.getId(), new FamilyCreateRequest("두 번째 가족")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ALREADY_FAMILY_MEMBER);
		assertThat(familyRepository.count()).isEqualTo(1);
	}

	@Test
	void createRejectsUnknownUser() {
		assertThatThrownBy(() -> familyService.create(999_999L, new FamilyCreateRequest("김씨네")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
		assertThat(familyRepository.count()).isZero();
	}

	@Test
	void createRetriesWhenInviteCodeIsAlreadyTaken() {
		User dad = saveUser("dad@example.com", "김아빠");
		willReturn("AAAAAA", "AAAAAA", "BBBBBB").given(inviteCodeGenerator).generate();
		familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		FamilyResponse response = familyService.create(dad.getId(), new FamilyCreateRequest("아빠네"));

		assertThat(response.inviteCode()).isEqualTo("BBBBBB");
	}

	@Test
	void hasFamilyIsTrueForUserInFamily() {
		familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThat(familyService.hasFamily(mom.getId())).isTrue();
	}

	@Test
	void hasFamilyIsFalseForUserWithoutFamily() {
		assertThat(familyService.hasFamily(mom.getId())).isFalse();
	}

	@Test
	void joinReturnsJoinedFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		FamilyResponse response = familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThat(response.id()).isEqualTo(created.id());
		assertThat(response.name()).isEqualTo("김씨네");
		assertThat(response.inviteCode()).isEqualTo(created.inviteCode());
	}

	@Test
	void joinRegistersUserAsMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThat(familyMemberRepository.findAll())
				.filteredOn(member -> member.getUser().getId().equals(dad.getId()))
				.singleElement()
				.satisfies(member -> {
					assertThat(member.getFamily().getId()).isEqualTo(created.id());
					assertThat(member.getRole()).isEqualTo(FamilyRole.MEMBER);
				});
		assertThat(familyMemberRepository.count()).isEqualTo(2);
	}

	@Test
	void joinedUserHasFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThat(familyService.hasFamily(dad.getId())).isTrue();
	}

	@Test
	void joinAcceptsLowercaseCodeWithSurroundingSpaces() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		String typed = " " + created.inviteCode().toLowerCase() + " ";

		FamilyResponse response = familyService.join(dad.getId(), new FamilyJoinRequest(typed));

		assertThat(response.id()).isEqualTo(created.id());
	}

	@Test
	void joinRejectsUnknownInviteCode() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.join(dad.getId(), new FamilyJoinRequest("000000")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVITE_CODE_NOT_FOUND);
		assertThat(familyService.hasFamily(dad.getId())).isFalse();
	}

	@Test
	void joinRejectsInviteCodeOfSuspendedFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		Family family = familyRepository.findById(created.id()).orElseThrow();
		ReflectionTestUtils.setField(family, "status", FamilyStatus.SUSPENDED);
		familyRepository.flush();

		assertThatThrownBy(() -> familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVITE_CODE_NOT_FOUND);
	}

	@Test
	void joinRejectsOwnerJoiningOwnFamilyAgain() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThatThrownBy(() -> familyService.join(mom.getId(), new FamilyJoinRequest(created.inviteCode())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ALREADY_FAMILY_MEMBER);
		assertThat(familyMemberRepository.count()).isEqualTo(1);
	}

	@Test
	void joinRejectsUserWhoAlreadyHasAnotherFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		familyService.create(dad.getId(), new FamilyCreateRequest("아빠네"));

		assertThatThrownBy(() -> familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ALREADY_FAMILY_MEMBER);
	}

	@Test
	void joinRejectsUnknownUser() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThatThrownBy(() -> familyService.join(999_999L, new FamilyJoinRequest(created.inviteCode())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
