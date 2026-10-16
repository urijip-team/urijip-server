package com.urijip.server.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.AdminFamilyListRequest;
import com.urijip.server.family.dto.request.AdminFamilyStatusRequest;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.AdminFamilyResponse;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.entity.FamilyStatus;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
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
class AdminFamilyServiceTest {

	@Autowired
	private AdminFamilyService adminFamilyService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private FamilyResponse kims;

	private FamilyResponse parks;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		User dad = saveUser("dad@example.com", "김아빠");
		User son = saveUser("son@example.com", "박아들");
		kims = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));
		familyService.join(dad.getId(), new FamilyJoinRequest(kims.inviteCode()));
		parks = familyService.create(son.getId(), new FamilyCreateRequest("박씨네"));
	}

	@Test
	void getFamiliesReturnsNewestFirstWithMemberCount() {
		PageResponse<AdminFamilyResponse> response = getFamilies(0, 20);

		assertThat(response.content()).extracting(AdminFamilyResponse::id).containsExactly(parks.id(), kims.id());
		assertThat(response.content()).extracting(AdminFamilyResponse::memberCount).containsExactly(1L, 2L);
		assertThat(response.totalElements()).isEqualTo(2);
		assertThat(response.totalPages()).isEqualTo(1);
		AdminFamilyResponse first = response.content().getFirst();
		assertThat(first.name()).isEqualTo("박씨네");
		assertThat(first.status()).isEqualTo(FamilyStatus.ACTIVE);
		assertThat(first.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
		assertThat(first.createdAt()).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));
	}

	@Test
	void getFamiliesPagesResults() {
		PageResponse<AdminFamilyResponse> second = getFamilies(1, 1);

		assertThat(second.content()).extracting(AdminFamilyResponse::id).containsExactly(kims.id());
		assertThat(second.page()).isEqualTo(1);
		assertThat(second.totalPages()).isEqualTo(2);
		assertThat(getFamilies(5, 20).content()).isEmpty();
	}

	@Test
	void getFamiliesUsesDefaultAndClampedPaging() {
		PageResponse<AdminFamilyResponse> defaults = getFamilies(null, null);
		PageResponse<AdminFamilyResponse> tooSmall = getFamilies(-1, 0);
		PageResponse<AdminFamilyResponse> tooLarge = getFamilies(0, 1000);

		assertThat(defaults.page()).isZero();
		assertThat(defaults.size()).isEqualTo(20);
		assertThat(tooSmall.page()).isZero();
		assertThat(tooSmall.size()).isEqualTo(1);
		assertThat(tooLarge.size()).isEqualTo(100);
	}

	@Test
	void changeStatusSuspendsAndReactivates() {
		AdminFamilyResponse suspended = adminFamilyService.changeStatus(kims.id(),
				new AdminFamilyStatusRequest(FamilyStatus.SUSPENDED));
		assertThat(suspended.status()).isEqualTo(FamilyStatus.SUSPENDED);
		assertThat(suspended.memberCount()).isEqualTo(2);
		assertThat(familyRepository.findById(kims.id()).orElseThrow().getStatus())
				.isEqualTo(FamilyStatus.SUSPENDED);

		AdminFamilyResponse active = adminFamilyService.changeStatus(kims.id(),
				new AdminFamilyStatusRequest(FamilyStatus.ACTIVE));
		assertThat(active.status()).isEqualTo(FamilyStatus.ACTIVE);
	}

	@Test
	void suspendedFamilyCannotBeJoined() {
		User daughter = saveUser("daughter@example.com", "김딸");
		adminFamilyService.changeStatus(kims.id(), new AdminFamilyStatusRequest(FamilyStatus.SUSPENDED));

		assertThatThrownBy(() -> familyService.join(daughter.getId(), new FamilyJoinRequest(kims.inviteCode())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVITE_CODE_NOT_FOUND);
	}

	@Test
	void changeStatusOfUnknownFamilyThrowsNotFound() {
		assertThatThrownBy(() -> adminFamilyService.changeStatus(999_999L,
				new AdminFamilyStatusRequest(FamilyStatus.SUSPENDED)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	private PageResponse<AdminFamilyResponse> getFamilies(Integer page, Integer size) {
		return adminFamilyService.getFamilies(new AdminFamilyListRequest(page, size));
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
