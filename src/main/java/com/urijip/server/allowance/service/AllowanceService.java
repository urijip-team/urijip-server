package com.urijip.server.allowance.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.urijip.server.allowance.dto.request.AllowanceCreateRequest;
import com.urijip.server.allowance.dto.response.AllowanceCreateResponse;
import com.urijip.server.allowance.dto.response.AllowanceDetailResponse;
import com.urijip.server.allowance.dto.response.AllowanceResponse;
import com.urijip.server.allowance.entity.AllowanceRequest;
import com.urijip.server.allowance.repository.AllowanceRequestRepository;
import com.urijip.server.chat.service.ChatService;
import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.service.PaymentInfoService;
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
	private final ChatService chatService;
	private final PaymentInfoService paymentInfoService;

	// 요청자와 받는 사람 모두 그 가족의 멤버여야 한다. 가족 채팅에 시스템 메시지를 남긴다.
	@Transactional
	public AllowanceCreateResponse create(Long userId, AllowanceCreateRequest request) {
		Map<Long, String> names = displayNames(request.familyId(), userId);
		if (userId.equals(request.receiverId())) {
			throw new BusinessException(ErrorCode.CANNOT_REQUEST_SELF);
		}
		familyService.validateMembers(request.familyId(), List.of(request.receiverId()));

		AllowanceRequest allowance = allowanceRequestRepository.save(new AllowanceRequest(
				familyService.getFamily(request.familyId()), userService.getUser(userId),
				userService.getUser(request.receiverId()), request.amount(), blankToNull(request.reason())));
		// 천 단위 쉼표가 JVM 로캘에 따라 바뀌지 않게 로캘을 고정한다.
		chatService.sendSystemMessage(request.familyId(), String.format(Locale.ROOT, "%s님이 용돈 %,d원을 요청했어요.",
				names.get(userId), request.amount()));
		return AllowanceCreateResponse.from(allowance);
	}

	// 요청자와 받는 사람만 볼 수 있다. 가족을 나간 뒤에도 볼 수 있다.
	@Transactional(readOnly = true)
	public AllowanceDetailResponse getDetail(Long requestId, Long userId) {
		AllowanceRequest allowance = getAllowance(requestId);
		if (!allowance.isParticipant(userId)) {
			throw new BusinessException(ErrorCode.NOT_ALLOWANCE_PARTICIPANT);
		}

		return AllowanceDetailResponse.of(AllowanceResponse.from(allowance),
				paymentInfoService.findPaymentInfo(allowance.getRequester().getId()).orElse(null));
	}

	private AllowanceRequest getAllowance(Long requestId) {
		return allowanceRequestRepository.findById(requestId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ALLOWANCE_NOT_FOUND));
	}

	// 멤버가 아니면 FAMILY_NOT_MEMBER. 호칭이 있으면 호칭, 없으면 이름이다.
	private Map<Long, String> displayNames(Long familyId, Long userId) {
		return familyService.getMembers(familyId, userId).stream()
				.collect(Collectors.toMap(FamilyMemberResponse::userId,
						member -> member.nickname() == null ? member.name() : member.nickname()));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

}
