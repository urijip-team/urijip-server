package com.urijip.server.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willReturn;

import java.util.List;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.request.FamilyOwnerTransferRequest;
import com.urijip.server.family.dto.response.FamilyMemberResponse;
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

	@Test
	void getMyFamilyReturnsFamilyOfOwner() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		FamilyResponse response = familyService.getMyFamily(mom.getId());

		assertThat(response).isEqualTo(created);
	}

	@Test
	void getMyFamilyReturnsFamilyOfJoinedMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThat(familyService.getMyFamily(dad.getId())).isEqualTo(created);
	}

	@Test
	void getMyFamilyRejectsUserWithoutFamily() {
		assertThatThrownBy(() -> familyService.getMyFamily(mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	void validateMemberPassesForOwner() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThatCode(() -> familyService.validateMember(created.id(), mom.getId())).doesNotThrowAnyException();
	}

	@Test
	void validateMemberPassesForJoinedMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThatCode(() -> familyService.validateMember(created.id(), dad.getId())).doesNotThrowAnyException();
	}

	@Test
	void validateMemberRejectsUserWithoutFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.validateMember(created.id(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void validateMemberRejectsMemberOfAnotherFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.create(dad.getId(), new FamilyCreateRequest("아빠네"));

		assertThatThrownBy(() -> familyService.validateMember(created.id(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void validateMemberRejectsUnknownFamilyId() {
		assertThatThrownBy(() -> familyService.validateMember(999_999L, mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getMembersReturnsOwnerThenJoinedMembers() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		List<FamilyMemberResponse> members = familyService.getMembers(created.id(), mom.getId());

		assertThat(members).containsExactly(
				new FamilyMemberResponse(mom.getId(), "김엄마", null, null, FamilyRole.OWNER),
				new FamilyMemberResponse(dad.getId(), "김아빠", null, null, FamilyRole.MEMBER));
	}

	@Test
	void getMembersIsAllowedForOrdinaryMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThat(familyService.getMembers(created.id(), dad.getId())).hasSize(2);
	}

	@Test
	void getMembersRejectsUserWhoIsNotMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.getMembers(created.id(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getMembersRejectsMemberOfAnotherFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		FamilyResponse another = familyService.create(dad.getId(), new FamilyCreateRequest("아빠네"));

		assertThatThrownBy(() -> familyService.getMembers(another.id(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.extracting(FamilyMemberResponse::name)
				.containsExactly("김엄마");
	}

	@Test
	void validateOwnerPassesForOwner() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThatCode(() -> familyService.validateOwner(created.id(), mom.getId())).doesNotThrowAnyException();
	}

	@Test
	void validateOwnerRejectsOrdinaryMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThatThrownBy(() -> familyService.validateOwner(created.id(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.NOT_FAMILY_OWNER);
	}

	@Test
	void validateOwnerRejectsUserWhoIsNotMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.validateOwner(created.id(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void reissueInviteCodeReturnsFamilyWithNewCode() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		FamilyResponse response = familyService.reissueInviteCode(created.id(), mom.getId());

		assertThat(response.id()).isEqualTo(created.id());
		assertThat(response.name()).isEqualTo("김씨네");
		assertThat(response.inviteCode()).matches("[A-HJ-NP-Z2-9]{6}").isNotEqualTo(created.inviteCode());
	}

	@Test
	void reissueInviteCodeSavesNewCode() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		FamilyResponse response = familyService.reissueInviteCode(created.id(), mom.getId());

		assertThat(familyRepository.existsByInviteCode(response.inviteCode())).isTrue();
		assertThat(familyRepository.existsByInviteCode(created.inviteCode())).isFalse();
		assertThat(familyService.getMyFamily(mom.getId())).isEqualTo(response);
	}

	@Test
	void oldInviteCodeCannotBeUsedAfterReissue() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.reissueInviteCode(created.id(), mom.getId());

		assertThatThrownBy(() -> familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVITE_CODE_NOT_FOUND);
	}

	@Test
	void newInviteCodeCanBeUsedAfterReissue() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		FamilyResponse reissued = familyService.reissueInviteCode(created.id(), mom.getId());

		familyService.join(dad.getId(), new FamilyJoinRequest(reissued.inviteCode()));

		assertThat(familyService.getMembers(created.id(), mom.getId())).hasSize(2);
	}

	@Test
	void reissueInviteCodeRejectsOrdinaryMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThatThrownBy(() -> familyService.reissueInviteCode(created.id(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.NOT_FAMILY_OWNER);
		assertThat(familyRepository.existsByInviteCode(created.inviteCode())).isTrue();
	}

	@Test
	void reissueInviteCodeRejectsUserWhoIsNotMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.reissueInviteCode(created.id(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
		assertThat(familyRepository.existsByInviteCode(created.inviteCode())).isTrue();
	}

	@Test
	void reissueInviteCodeRejectsUnknownFamilyId() {
		assertThatThrownBy(() -> familyService.reissueInviteCode(999_999L, mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void transferOwnerSwapsRoles() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		List<FamilyMemberResponse> members = familyService.transferOwner(created.id(), mom.getId(), new FamilyOwnerTransferRequest(dad.getId()));

		assertThat(members).containsExactly(
				new FamilyMemberResponse(mom.getId(), "김엄마", null, null, FamilyRole.MEMBER),
				new FamilyMemberResponse(dad.getId(), "김아빠", null, null, FamilyRole.OWNER));
	}

	@Test
	void transferOwnerChangesOwnerOfFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		familyService.transferOwner(created.id(), mom.getId(), new FamilyOwnerTransferRequest(dad.getId()));

		Family family = familyRepository.findById(created.id()).orElseThrow();
		assertThat(family.getOwner().getId()).isEqualTo(dad.getId());
	}

	@Test
	void transferOwnerMovesOwnerPermission() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));
		familyService.transferOwner(created.id(), mom.getId(), new FamilyOwnerTransferRequest(dad.getId()));

		assertThatThrownBy(() -> familyService.reissueInviteCode(created.id(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.NOT_FAMILY_OWNER);
		assertThatCode(() -> familyService.reissueInviteCode(created.id(), dad.getId())).doesNotThrowAnyException();
	}

	@Test
	void transferOwnerRejectsOrdinaryMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThatThrownBy(() -> familyService.transferOwner(created.id(), dad.getId(), new FamilyOwnerTransferRequest(dad.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.NOT_FAMILY_OWNER);
		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.filteredOn(member -> member.role() == FamilyRole.OWNER)
				.extracting(FamilyMemberResponse::userId)
				.containsExactly(mom.getId());
	}

	@Test
	void transferOwnerRejectsTargetWhoIsNotMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.transferOwner(created.id(), mom.getId(), new FamilyOwnerTransferRequest(dad.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_MEMBER_NOT_FOUND);
		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.filteredOn(member -> member.role() == FamilyRole.OWNER)
				.extracting(FamilyMemberResponse::userId)
				.containsExactly(mom.getId());
	}

	@Test
	void transferOwnerRejectsTransferToSelf() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThatThrownBy(() -> familyService.transferOwner(created.id(), mom.getId(), new FamilyOwnerTransferRequest(mom.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);
		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.filteredOn(member -> member.role() == FamilyRole.OWNER)
				.extracting(FamilyMemberResponse::userId)
				.containsExactly(mom.getId());
	}

	@Test
	void transferOwnerRejectsRequesterWhoIsNotMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.transferOwner(created.id(), dad.getId(), new FamilyOwnerTransferRequest(mom.getId())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void ownerRemovesMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		familyService.removeMember(created.id(), mom.getId(), dad.getId());

		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.extracting(FamilyMemberResponse::name)
				.containsExactly("김엄마");
		assertThat(familyService.hasFamily(dad.getId())).isFalse();
	}

	@Test
	void ordinaryMemberCannotRemoveAnotherMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		User kid = saveUser("kid@example.com", "김첫째");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));
		familyService.join(kid.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThatThrownBy(() -> familyService.removeMember(created.id(), dad.getId(), kid.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.NOT_FAMILY_OWNER);
		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.extracting(FamilyMemberResponse::name)
				.containsExactly("김엄마", "김아빠", "김첫째");
	}

	@Test
	void ordinaryMemberCannotRemoveOwner() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThatThrownBy(() -> familyService.removeMember(created.id(), dad.getId(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.NOT_FAMILY_OWNER);
		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.extracting(FamilyMemberResponse::name)
				.containsExactly("김엄마", "김아빠");
	}

	@Test
	void ownerCannotRemoveUserWhoIsNotMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.removeMember(created.id(), mom.getId(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_MEMBER_NOT_FOUND);
	}

	@Test
	void outsiderCannotRemoveMember() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.removeMember(created.id(), dad.getId(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.extracting(FamilyMemberResponse::name)
				.containsExactly("김엄마");
	}

	@Test
	void ordinaryMemberLeavesFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		familyService.removeMember(created.id(), dad.getId(), dad.getId());

		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.extracting(FamilyMemberResponse::name)
				.containsExactly("김엄마");
		assertThat(familyService.hasFamily(dad.getId())).isFalse();
		assertThat(familyRepository.count()).isEqualTo(1);
	}

	@Test
	void memberWhoLeftCanJoinAgain() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));
		familyService.removeMember(created.id(), dad.getId(), dad.getId());

		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.extracting(FamilyMemberResponse::name)
				.containsExactly("김엄마", "김아빠");
	}

	@Test
	void outsiderCannotLeaveFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");

		assertThatThrownBy(() -> familyService.removeMember(created.id(), dad.getId(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void ownerCannotLeaveWhileOtherMembersRemain() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));

		assertThatThrownBy(() -> familyService.removeMember(created.id(), mom.getId(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.OWNER_CANNOT_LEAVE);
		assertThat(familyService.getMembers(created.id(), mom.getId()))
				.extracting(FamilyMemberResponse::name)
				.containsExactly("김엄마", "김아빠");
	}

	@Test
	void formerOwnerCanLeaveAfterTransferringOwner() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		User dad = saveUser("dad@example.com", "김아빠");
		familyService.join(dad.getId(), new FamilyJoinRequest(created.inviteCode()));
		familyService.transferOwner(created.id(), mom.getId(), new FamilyOwnerTransferRequest(dad.getId()));

		familyService.removeMember(created.id(), mom.getId(), mom.getId());

		assertThat(familyService.getMembers(created.id(), dad.getId()))
				.containsExactly(new FamilyMemberResponse(dad.getId(), "김아빠", null, null, FamilyRole.OWNER));
		assertThat(familyService.hasFamily(mom.getId())).isFalse();
	}

	@Test
	void lastOwnerLeavingDeletesFamily() {
		FamilyResponse created = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		familyService.removeMember(created.id(), mom.getId(), mom.getId());

		assertThat(familyRepository.count()).isZero();
		assertThat(familyMemberRepository.count()).isZero();
		assertThat(familyService.hasFamily(mom.getId())).isFalse();
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
