package com.urijip.server.adminlog.service;

import com.urijip.server.adminlog.dto.request.AdminLogSearchRequest;
import com.urijip.server.adminlog.dto.response.AdminLogResponse;
import com.urijip.server.adminlog.entity.AdminAction;
import com.urijip.server.adminlog.entity.AdminLog;
import com.urijip.server.adminlog.entity.AdminLogTargetType;
import com.urijip.server.adminlog.repository.AdminLogRepository;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.member.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLogService {

	private final AdminLogRepository adminLogRepository;
	private final UserService userService;

	// 관리자 서비스가 변경을 마친 뒤 같은 트랜잭션에서 부른다. 작업이 롤백되면 기록도 남지 않는다.
	@Transactional
	public void record(Long adminId, AdminAction action, AdminLogTargetType targetType, Long targetId, String detail) {
		adminLogRepository.save(new AdminLog(userService.getUser(adminId), action, targetType, targetId, detail));
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminLogResponse> search(AdminLogSearchRequest request) {
		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault());
		return PageResponse.from(adminLogRepository.search(request.adminId(), pageable).map(AdminLogResponse::from));
	}

}
