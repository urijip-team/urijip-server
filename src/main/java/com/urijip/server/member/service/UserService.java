package com.urijip.server.member.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.member.repository.DailySignupCount;
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

	// 관리자 계정과 탈퇴한 회원은 세지 않는다. 정지된 회원은 센다.
	@Transactional(readOnly = true)
	public long countUsers() {
		return userRepository.countByRoleAndStatusNot(Role.USER, UserStatus.WITHDRAWN);
	}

	// from부터 to까지(둘 다 포함) 날짜별 가입 수. 가입이 없는 날은 빠지고, 날짜 순이다.
	@Transactional(readOnly = true)
	public Map<LocalDate, Long> countSignupsByDay(LocalDate from, LocalDate to) {
		Map<LocalDate, Long> counts = new LinkedHashMap<>();
		for (DailySignupCount count : userRepository.countSignupsByDay(Role.USER, from.atStartOfDay(),
				to.plusDays(1).atStartOfDay())) {
			counts.put(count.getDay(), count.getCount());
		}
		return counts;
	}

}
