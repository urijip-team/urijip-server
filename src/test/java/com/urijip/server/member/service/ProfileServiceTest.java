package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.dto.request.ProfileUpdateRequest;
import com.urijip.server.member.dto.response.ProfileResponse;
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
class ProfileServiceTest {

	@Autowired
	private ProfileService profileService;

	@Autowired
	private FamilyService familyService;

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
	void getProfileReturnsProfileOfFamilyMember() {
		ReflectionTestUtils.setField(mom, "profileImageUrl", "https://example.com/mom.png");
		ReflectionTestUtils.setField(mom, "birthday", LocalDate.of(1980, 5, 5));
		ReflectionTestUtils.setField(mom, "statusMessage", "오늘도 화이팅");
		joinSameFamily();

		ProfileResponse response = profileService.getProfile(dad.getId(), mom.getId());

		assertThat(response).isEqualTo(new ProfileResponse(mom.getId(), "김엄마", "https://example.com/mom.png",
				LocalDate.of(1980, 5, 5), "오늘도 화이팅"));
	}

	@Test
	void getProfileReturnsNullForFieldsNotSetYet() {
		joinSameFamily();

		ProfileResponse response = profileService.getProfile(mom.getId(), dad.getId());

		assertThat(response).isEqualTo(new ProfileResponse(dad.getId(), "김아빠", null, null, null));
	}

	@Test
	void getProfileReturnsOwnProfileWithoutFamily() {
		ProfileResponse response = profileService.getProfile(mom.getId(), mom.getId());

		assertThat(response.id()).isEqualTo(mom.getId());
		assertThat(response.name()).isEqualTo("김엄마");
	}

	@Test
	void getProfileRejectsUserOutsideFamily() {
		familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThatThrownBy(() -> profileService.getProfile(mom.getId(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getProfileRejectsRequesterWithoutFamily() {
		familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThatThrownBy(() -> profileService.getProfile(dad.getId(), mom.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getProfileRejectsMemberWhoLeftFamily() {
		FamilyResponse family = joinSameFamily();
		familyService.removeMember(family.id(), dad.getId(), dad.getId());

		assertThatThrownBy(() -> profileService.getProfile(mom.getId(), dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void getProfileOfUnknownUserIsRejectedLikeOutsider() {
		familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThatThrownBy(() -> profileService.getProfile(mom.getId(), 999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void updateMyProfileChangesAllFields() {
		ProfileResponse response = profileService.updateMyProfile(mom.getId(),
				new ProfileUpdateRequest("김영희", LocalDate.of(1980, 5, 5), "오늘도 화이팅"));

		assertThat(response).isEqualTo(new ProfileResponse(mom.getId(), "김영희", null, LocalDate.of(1980, 5, 5),
				"오늘도 화이팅"));
		assertThat(profileService.getProfile(mom.getId(), mom.getId())).isEqualTo(response);
	}

	@Test
	void updateMyProfileKeepsFieldsThatAreNotSent() {
		profileService.updateMyProfile(mom.getId(), new ProfileUpdateRequest("김영희", LocalDate.of(1980, 5, 5), "오늘도 화이팅"));

		ProfileResponse response = profileService.updateMyProfile(mom.getId(),
				new ProfileUpdateRequest(null, null, "퇴근 중"));

		assertThat(response).isEqualTo(new ProfileResponse(mom.getId(), "김영희", null, LocalDate.of(1980, 5, 5), "퇴근 중"));
	}

	@Test
	void updateMyProfileClearsStatusMessageWithBlank() {
		profileService.updateMyProfile(mom.getId(), new ProfileUpdateRequest(null, null, "오늘도 화이팅"));

		ProfileResponse response = profileService.updateMyProfile(mom.getId(),
				new ProfileUpdateRequest(null, null, ""));

		assertThat(response.statusMessage()).isNull();
	}

	@Test
	void updateMyProfileDoesNotChangeOtherUsers() {
		profileService.updateMyProfile(mom.getId(), new ProfileUpdateRequest("김영희", null, null));

		assertThat(profileService.getProfile(dad.getId(), dad.getId()).name()).isEqualTo("김아빠");
	}

	@Test
	void updateMyProfileRejectsUnknownUser() {
		assertThatThrownBy(() -> profileService.updateMyProfile(999_999L,
				new ProfileUpdateRequest("김영희", null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
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
