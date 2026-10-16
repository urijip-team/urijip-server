package com.urijip.server.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.member.dto.request.AdminUserSearchRequest;
import com.urijip.server.member.dto.request.AdminUserStatusRequest;
import com.urijip.server.member.dto.response.AdminUserDetailResponse;
import com.urijip.server.member.dto.response.AdminUserResponse;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
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
class AdminUserServiceTest {

	@Autowired
	private AdminUserService adminUserService;

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
	void searchReturnsUsersNewestFirstWithCreatedAtInSeoulTime() {
		PageResponse<AdminUserResponse> response = search(null, null, 0, 20);

		assertThat(response.content()).extracting(AdminUserResponse::id).containsExactly(dad.getId(), mom.getId());
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isEqualTo(2);
		assertThat(response.totalPages()).isEqualTo(1);
		AdminUserResponse first = response.content().getFirst();
		assertThat(first.email()).isEqualTo("dad@example.com");
		assertThat(first.status()).isEqualTo(UserStatus.ACTIVE);
		assertThat(first.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
		assertThat(first.createdAt()).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.MINUTES));
	}

	@Test
	void searchByKeywordAndStatus() {
		adminUserService.changeStatus(dad.getId(), new AdminUserStatusRequest(UserStatus.SUSPENDED));

		assertThat(search("아빠", null, 0, 20).content()).extracting(AdminUserResponse::id)
				.containsExactly(dad.getId());
		assertThat(search(null, UserStatus.ACTIVE, 0, 20).content())
				.extracting(AdminUserResponse::id)
				.containsExactly(mom.getId());
		assertThat(search("엄마", UserStatus.SUSPENDED, 0, 20).content()).isEmpty();
	}

	@Test
	void searchTreatsWildcardsAndBlankKeywordSafely() {
		assertThat(search("%", null, 0, 20).content()).isEmpty();
		assertThat(search("_", null, 0, 20).content()).isEmpty();
		assertThat(search("  ", null, 0, 20).content()).hasSize(2);
	}

	@Test
	void searchClampsPageAndSize() {
		PageResponse<AdminUserResponse> tooSmall = search(null, null, -1, 0);
		PageResponse<AdminUserResponse> tooLarge = search(null, null, 0, 1000);

		assertThat(tooSmall.page()).isZero();
		assertThat(tooSmall.size()).isEqualTo(1);
		assertThat(tooLarge.size()).isEqualTo(100);
	}

	@Test
	void searchUsesDefaultPageAndSize() {
		PageResponse<AdminUserResponse> response = search(null, null, null, null);

		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(20);
	}

	@Test
	void getUserReturnsDetailWithFamily() {
		ReflectionTestUtils.setField(mom, "statusMessage", "오늘도 화이팅");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		AdminUserDetailResponse response = adminUserService.getUser(mom.getId());

		assertThat(response.id()).isEqualTo(mom.getId());
		assertThat(response.email()).isEqualTo("mom@example.com");
		assertThat(response.name()).isEqualTo("김엄마");
		assertThat(response.statusMessage()).isEqualTo("오늘도 화이팅");
		assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
		assertThat(response.createdAt()).isNotNull();
		assertThat(response.family()).isEqualTo(new AdminUserDetailResponse.Family(family.id(), "김씨네"));
	}

	@Test
	void getUserWithoutFamilyHasNullFamily() {
		assertThat(adminUserService.getUser(dad.getId()).family()).isNull();
	}

	@Test
	void getUserRejectsUnknownUserAndAdmin() {
		makeAdmin(dad);

		assertThatThrownBy(() -> adminUserService.getUser(999_999L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
		assertThatThrownBy(() -> adminUserService.getUser(dad.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	void changeStatusSuspendsAndReactivates() {
		AdminUserDetailResponse suspended = adminUserService.changeStatus(mom.getId(),
				new AdminUserStatusRequest(UserStatus.SUSPENDED));
		assertThat(suspended.status()).isEqualTo(UserStatus.SUSPENDED);
		assertThat(userRepository.findById(mom.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.SUSPENDED);

		AdminUserDetailResponse active = adminUserService.changeStatus(mom.getId(),
				new AdminUserStatusRequest(UserStatus.ACTIVE));
		assertThat(active.status()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void changeStatusRejectsWithdrawnAsTargetStatus() {
		assertThatThrownBy(() -> adminUserService.changeStatus(mom.getId(),
				new AdminUserStatusRequest(UserStatus.WITHDRAWN)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_INPUT);
	}

	@Test
	void changeStatusRejectsWithdrawnUser() {
		mom.withdraw();
		userRepository.flush();

		assertThatThrownBy(() -> adminUserService.changeStatus(mom.getId(),
				new AdminUserStatusRequest(UserStatus.ACTIVE)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_WITHDRAWN);
	}

	@Test
	void changeStatusRejectsAdmin() {
		makeAdmin(dad);

		assertThatThrownBy(() -> adminUserService.changeStatus(dad.getId(),
				new AdminUserStatusRequest(UserStatus.SUSPENDED)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	private PageResponse<AdminUserResponse> search(String keyword, UserStatus status, Integer page, Integer size) {
		return adminUserService.search(new AdminUserSearchRequest(keyword, status, page, size));
	}

	private void makeAdmin(User user) {
		ReflectionTestUtils.setField(user, "role", Role.ADMIN);
		userRepository.flush();
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
