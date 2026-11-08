package com.urijip.server.notice.service;

import com.urijip.server.adminlog.entity.AdminAction;
import com.urijip.server.adminlog.entity.AdminLogTargetType;
import com.urijip.server.adminlog.service.AdminLogService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.member.service.UserService;
import com.urijip.server.notice.dto.request.AdminNoticeCreateRequest;
import com.urijip.server.notice.dto.request.AdminNoticeUpdateRequest;
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
	private final AdminLogService adminLogService;

	@Transactional
	public AdminNoticeResponse create(Long adminId, AdminNoticeCreateRequest request) {
		Notice notice = noticeRepository.save(new Notice(userService.getUser(adminId), request.title(),
				request.content()));
		adminLogService.record(adminId, AdminAction.CREATE_NOTICE, AdminLogTargetType.NOTICE, notice.getId(),
				notice.getTitle());
		return AdminNoticeResponse.from(notice);
	}

	@Transactional
	public AdminNoticeResponse update(Long noticeId, Long adminId, AdminNoticeUpdateRequest request) {
		Notice notice = getNotice(noticeId);
		notice.update(request.title(), request.content());
		return AdminNoticeResponse.from(notice);
	}

	@Transactional
	public void delete(Long noticeId, Long adminId) {
		noticeRepository.delete(getNotice(noticeId));
	}

	private Notice getNotice(Long noticeId) {
		return noticeRepository.findById(noticeId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
	}

}
