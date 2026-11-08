package com.urijip.server.allowance.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.urijip.server.allowance.dto.request.AllowanceCreateRequest;
import com.urijip.server.allowance.dto.request.AllowanceHistoryType;
import com.urijip.server.allowance.dto.request.AllowanceStatusRequest;
import com.urijip.server.allowance.dto.response.AllowanceCreateResponse;
import com.urijip.server.allowance.dto.response.AllowanceDetailResponse;
import com.urijip.server.allowance.dto.response.AllowanceMonthlyResponse;
import com.urijip.server.allowance.dto.response.AllowanceResponse;
import com.urijip.server.allowance.entity.AllowanceRequest;
import com.urijip.server.allowance.entity.AllowanceStatus;
import com.urijip.server.allowance.repository.AllowanceRequestRepository;
import com.urijip.server.chat.service.ChatService;
import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.service.FamilyDeletedEvent;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.push.PushMessage;
import com.urijip.server.global.push.PushType;
import com.urijip.server.member.service.PaymentInfoService;
import com.urijip.server.member.service.UserService;
import com.urijip.server.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
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
	private final NotificationService notificationService;

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
		String message = String.format(Locale.ROOT, "%s님이 용돈 %,d원을 요청했어요.", names.get(userId), request.amount());
		chatService.sendSystemMessage(request.familyId(), message);
		push(request.receiverId(), "용돈 요청", message, allowance);
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

	// 받는 사람만 한 번 처리할 수 있다. 처리 결과를 요청자에게 푸시한다.
	@Transactional
	public AllowanceResponse changeStatus(Long requestId, Long userId, AllowanceStatusRequest request) {
		if (request.status() == AllowanceStatus.REQUESTED) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		AllowanceRequest allowance = getAllowance(requestId);
		if (!allowance.isReceiver(userId)) {
			throw new BusinessException(ErrorCode.NOT_ALLOWANCE_RECEIVER);
		}
		if (allowance.isHandled()) {
			throw new BusinessException(ErrorCode.ALLOWANCE_ALREADY_HANDLED);
		}

		allowance.complete(request.status());
		pushResult(allowance);
		return AllowanceResponse.from(allowance);
	}

	// 그 달(서울 시간)에 만든 요청 중 내가 보냈거나(sent) 받은(received) 것. 가족을 옮겨도 모두 보인다.
	@Transactional(readOnly = true)
	public AllowanceMonthlyResponse getMonthly(Long userId, YearMonth month, String type) {
		AllowanceHistoryType historyType = AllowanceHistoryType.from(type);
		LocalDateTime from = month.atDay(1).atStartOfDay();
		LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();

		List<AllowanceRequest> requests = historyType == AllowanceHistoryType.SENT
				? allowanceRequestRepository.findSent(userId, from, to)
				: allowanceRequestRepository.findReceived(userId, from, to);
		return AllowanceMonthlyResponse.from(requests.stream().map(AllowanceResponse::from).toList());
	}

	@EventListener
	public void onFamilyDeleted(FamilyDeletedEvent event) {
		allowanceRequestRepository.deleteByFamilyId(event.familyId());
	}

	// 처리할 때는 가족을 나갔을 수도 있어 호칭 대신 이름을 쓴다.
	private void pushResult(AllowanceRequest allowance) {
		String receiverName = allowance.getReceiver().getName();
		if (allowance.getStatus() == AllowanceStatus.SENT) {
			push(allowance.getRequester().getId(), "용돈 도착", String.format(Locale.ROOT,
					"%s님이 용돈 %,d원을 보냈어요.", receiverName, allowance.getAmount()), allowance);
		}
	}

	private void push(Long receiverId, String title, String body, AllowanceRequest allowance) {
		notificationService.send(List.of(receiverId), new PushMessage(PushType.ALLOWANCE, title, body,
				Map.of("requestId", allowance.getId().toString())));
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
