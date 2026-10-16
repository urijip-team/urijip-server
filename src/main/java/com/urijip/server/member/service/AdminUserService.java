package com.urijip.server.member.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자가 다루는 대상은 일반 회원(USER)뿐이다. 관리자 계정은 목록에 나오지 않고 조회·정지할 수 없다.
@Service
@RequiredArgsConstructor
public class AdminUserService {

	private final UserRepository userRepository;
	private final FamilyService familyService;

	@Transactional(readOnly = true)
	public PageResponse<AdminUserResponse> search(AdminUserSearchRequest request) {
		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(),
				Sort.by(Sort.Direction.DESC, "id"));

		return PageResponse.from(userRepository.search(Role.USER, request.status(), escapeLike(request.keyword()),
				pageable).map(AdminUserResponse::from));
	}

	@Transactional(readOnly = true)
	public AdminUserDetailResponse getUser(Long userId) {
		User user = getManagedUser(userId);
		return AdminUserDetailResponse.of(user, familyService.findFamilyOf(userId).orElse(null));
	}

	@Transactional
	public AdminUserDetailResponse changeStatus(Long userId, AdminUserStatusRequest request) {
		if (request.status() == UserStatus.WITHDRAWN) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		User user = getManagedUser(userId);
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			throw new BusinessException(ErrorCode.USER_WITHDRAWN);
		}

		user.changeStatus(request.status());

		return AdminUserDetailResponse.of(user, familyService.findFamilyOf(userId).orElse(null));
	}

	private User getManagedUser(Long userId) {
		return userRepository.findById(userId)
				.filter(user -> user.getRole() == Role.USER)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	private String escapeLike(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		return keyword.trim()
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");
	}

}
