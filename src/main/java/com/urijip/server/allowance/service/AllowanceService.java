package com.urijip.server.allowance.service;

import java.util.List;

import com.urijip.server.allowance.dto.request.AllowanceCreateRequest;
import com.urijip.server.allowance.dto.response.AllowanceCreateResponse;
import com.urijip.server.allowance.entity.AllowanceRequest;
import com.urijip.server.allowance.repository.AllowanceRequestRepository;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AllowanceService {

	private final AllowanceRequestRepository allowanceRequestRepository;
	private final FamilyService familyService;
	private final UserService userService;

	// 요청자와 받는 사람 모두 그 가족의 멤버여야 한다.
	@Transactional
	public AllowanceCreateResponse create(Long userId, AllowanceCreateRequest request) {
		familyService.validateMember(request.familyId(), userId);
		if (userId.equals(request.receiverId())) {
			throw new BusinessException(ErrorCode.CANNOT_REQUEST_SELF);
		}
		familyService.validateMembers(request.familyId(), List.of(request.receiverId()));

		AllowanceRequest allowance = allowanceRequestRepository.save(new AllowanceRequest(
				familyService.getFamily(request.familyId()), userService.getUser(userId),
				userService.getUser(request.receiverId()), request.amount(), blankToNull(request.reason())));
		return AllowanceCreateResponse.from(allowance);
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

}
