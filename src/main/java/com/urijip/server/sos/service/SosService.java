package com.urijip.server.sos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.urijip.server.family.service.FamilyService;
import com.urijip.server.member.service.UserService;
import com.urijip.server.sos.dto.request.SosSendRequest;
import com.urijip.server.sos.dto.response.SosCreateResponse;
import com.urijip.server.sos.entity.SosAlert;
import com.urijip.server.sos.repository.SosAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SosService {

	private static final int SCALE = 7;

	private final SosAlertRepository sosAlertRepository;
	private final FamilyService familyService;
	private final UserService userService;

	// 위치 공유를 꺼 둔 회원도 보낼 수 있다. 위치는 이 SOS 기록에만 남고 user_locations는 바꾸지 않는다.
	@Transactional
	public SosCreateResponse send(Long userId, SosSendRequest request) {
		familyService.validateMember(request.familyId(), userId);
		SosAlert alert = sosAlertRepository.save(new SosAlert(familyService.getFamily(request.familyId()),
				userService.getUser(userId), scale(request.latitude()), scale(request.longitude())));
		return SosCreateResponse.from(alert);
	}

	private BigDecimal scale(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}

}
