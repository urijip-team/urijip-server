package com.urijip.server.member.service;

import java.util.Collection;
import java.util.List;

import com.urijip.server.member.dto.request.DeviceTokenRequest;
import com.urijip.server.member.entity.DeviceToken;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

	private final DeviceTokenRepository deviceTokenRepository;
	private final UserService userService;

	@Transactional
	public void register(Long userId, DeviceTokenRequest request) {
		User user = userService.getUser(userId);

		deviceTokenRepository.findByFcmToken(request.fcmToken()).ifPresentOrElse(
				saved -> saved.refresh(user, request.platform()),
				() -> deviceTokenRepository
						.save(new DeviceToken(user, request.fcmToken(), request.platform())));
	}

	// 푸시를 받을 회원들의 기기 토큰. 한 회원이 기기를 여러 대 쓰면 토큰도 여러 개다.
	@Transactional(readOnly = true)
	public List<String> getTokens(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return List.of();
		}
		return deviceTokenRepository.findFcmTokensByUserIds(userIds);
	}

	// 전체 푸시를 받을 기기 토큰. 관리자와 정지·탈퇴한 회원, excludedUserIds(알림을 끈 회원)는 뺀다.
	@Transactional(readOnly = true)
	public List<String> getActiveUserTokens(Collection<Long> excludedUserIds) {
		if (excludedUserIds.isEmpty()) {
			return deviceTokenRepository.findFcmTokensByUserRoleAndStatus(Role.USER, UserStatus.ACTIVE);
		}
		return deviceTokenRepository.findFcmTokensByUserRoleAndStatusExcluding(Role.USER, UserStatus.ACTIVE,
				excludedUserIds);
	}

	// FCM이 다시 쓸 수 없다고 알려 준 토큰을 지운다. 누구의 토큰인지는 따지지 않는다.
	@Transactional
	public void deleteTokens(Collection<String> fcmTokens) {
		if (!fcmTokens.isEmpty()) {
			deviceTokenRepository.deleteByFcmTokens(fcmTokens);
		}
	}

}
