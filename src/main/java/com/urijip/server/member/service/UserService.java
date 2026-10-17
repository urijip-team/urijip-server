package com.urijip.server.member.service;

import java.util.Collection;
import java.util.List;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public User getUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	// 없는 id는 건너뛴다.
	@Transactional(readOnly = true)
	public List<User> getUsers(Collection<Long> userIds) {
		return userRepository.findAllById(userIds);
	}

}
