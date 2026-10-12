package com.urijip.server.member.service;

import com.urijip.server.member.dto.request.DeviceTokenRequest;
import com.urijip.server.member.entity.DeviceToken;
import com.urijip.server.member.entity.User;
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

}
