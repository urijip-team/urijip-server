package com.urijip.server.notice.service;

import com.urijip.server.member.service.UserService;
import com.urijip.server.notice.dto.request.AdminNoticeCreateRequest;
import com.urijip.server.notice.dto.response.AdminNoticeResponse;
import com.urijip.server.notice.entity.Notice;
import com.urijip.server.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminNoticeService {

	private final NoticeRepository noticeRepository;
	private final UserService userService;

	@Transactional
	public AdminNoticeResponse create(Long adminId, AdminNoticeCreateRequest request) {
		Notice notice = new Notice(userService.getUser(adminId), request.title(), request.content());
		return AdminNoticeResponse.from(noticeRepository.save(notice));
	}

}
