package com.urijip.server.sos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

import com.urijip.server.chat.service.ChatService;
import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.service.UserService;
import com.urijip.server.sos.dto.request.SosSendRequest;
import com.urijip.server.sos.dto.response.SosCreateResponse;
import com.urijip.server.sos.dto.response.SosResponse;
import com.urijip.server.sos.entity.SosAlert;
import com.urijip.server.sos.repository.SosAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SosService {

	private static final int SCALE = 7;

	private final SosAlertRepository sosAlertRepository;
	private final FamilyService familyService;
	private final UserService userService;
	private final ChatService chatService;
	private final ApplicationEventPublisher eventPublisher;

	// 위치 공유를 꺼 둔 회원도 보낼 수 있다. 위치는 이 SOS 기록에만 남고 user_locations는 바꾸지 않는다.
	@Transactional
	public SosCreateResponse send(Long userId, SosSendRequest request) {
		Map<Long, String> names = displayNames(request.familyId(), userId);
		SosAlert alert = sosAlertRepository.save(new SosAlert(familyService.getFamily(request.familyId()),
				userService.getUser(userId), scale(request.latitude()), scale(request.longitude())));
		publishChanged(alert, names);
		chatService.sendSystemMessage(request.familyId(), names.get(userId) + "님이 SOS를 보냈습니다.");
		return SosCreateResponse.from(alert);
	}

	// 같은 가족이면 보낸 사람이 아니어도 해제할 수 있다.
	@Transactional
	public SosResponse resolve(Long sosId, Long userId) {
		SosAlert alert = sosAlertRepository.findById(sosId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SOS_NOT_FOUND));
		Map<Long, String> names = displayNames(alert.getFamily().getId(), userId);
		if (alert.isResolved()) {
			throw new BusinessException(ErrorCode.SOS_ALREADY_RESOLVED);
		}

		alert.resolve(userService.getUser(userId));
		return SosResponse.of(alert, names);
	}

	// 멤버가 아니면 FAMILY_NOT_MEMBER.
	private Map<Long, String> displayNames(Long familyId, Long userId) {
		return familyService.getMembers(familyId, userId).stream()
				.collect(Collectors.toMap(FamilyMemberResponse::userId,
						member -> member.nickname() == null ? member.name() : member.nickname()));
	}

	private void publishChanged(SosAlert alert, Map<Long, String> names) {
		eventPublisher.publishEvent(new SosChangedEvent(alert.getFamily().getId(), SosResponse.of(alert, names)));
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}

}
