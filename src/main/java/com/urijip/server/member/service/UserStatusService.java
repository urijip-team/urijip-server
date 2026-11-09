package com.urijip.server.member.service;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.security.UserStatusChecker;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 인증 필터와 STOMP 인터셉터가 요청마다 부른다.
@Service
@RequiredArgsConstructor
public class UserStatusService implements UserStatusChecker {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public void validateActive(Long userId) {
		UserStatus status = userRepository.findStatusById(userId).orElse(UserStatus.WITHDRAWN);
		if (status == UserStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.USER_SUSPENDED);
		}
		if (status == UserStatus.WITHDRAWN) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
	}

}
