package com.urijip.server.notice.service;

import java.util.Map;

import com.urijip.server.adminlog.entity.AdminAction;
import com.urijip.server.adminlog.entity.AdminLogTargetType;
import com.urijip.server.adminlog.service.AdminLogService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.push.PushMessage;
import com.urijip.server.global.push.PushType;
import com.urijip.server.member.service.UserService;
import com.urijip.server.notice.dto.request.AdminNoticeCreateRequest;
import com.urijip.server.notice.dto.request.AdminNoticeUpdateRequest;
import com.urijip.server.notice.dto.response.AdminNoticeResponse;
import com.urijip.server.notice.entity.Notice;
import com.urijip.server.notice.repository.NoticeRepository;
import com.urijip.server.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminNoticeService {

	private final NoticeRepository noticeRepository;
	private final UserService userService;
	private final AdminLogService adminLogService;
	private final NotificationService notificationService;

	// push가 true면 활동 중인 모든 회원에게 공지 제목으로 푸시한다.
	@Transactional
	public AdminNoticeResponse create(Long adminId, AdminNoticeCreateRequest request) {
		Notice notice = noticeRepository.save(new Notice(userService.getUser(adminId), request.title(),
				request.content()));
		adminLogService.record(adminId, AdminAction.CREATE_NOTICE, AdminLogTargetType.NOTICE, notice.getId(),
				notice.getTitle());
		if (request.pushRequested()) {
			pushToAll(notice);
		}
		return AdminNoticeResponse.from(notice);
	}

	// push가 true면 수정한 공지의 제목으로 다시 전체 푸시한다(명세서: 작성·수정 시 push: true면 전체 푸시).
	@Transactional
	public AdminNoticeResponse update(Long noticeId, Long adminId, AdminNoticeUpdateRequest request) {
		Notice notice = getNotice(noticeId);
		notice.update(request.title(), request.content());
		adminLogService.record(adminId, AdminAction.UPDATE_NOTICE, AdminLogTargetType.NOTICE, noticeId,
				notice.getTitle());
		if (request.pushRequested()) {
			pushToAll(notice);
		}
		return AdminNoticeResponse.from(notice);
	}

	@Transactional
	public void delete(Long noticeId, Long adminId) {
		Notice notice = getNotice(noticeId);
		noticeRepository.delete(notice);
		// 공지가 지워져도 기록에서 무엇을 지웠는지 알 수 있게 제목을 남긴다.
		adminLogService.record(adminId, AdminAction.DELETE_NOTICE, AdminLogTargetType.NOTICE, noticeId,
				notice.getTitle());
	}

	private void pushToAll(Notice notice) {
		notificationService.sendToAll(new PushMessage(PushType.NOTICE, "공지사항", notice.getTitle(),
				Map.of("noticeId", notice.getId().toString())));
		notice.markPushed();
	}

	private Notice getNotice(Long noticeId) {
		return noticeRepository.findById(noticeId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
	}

}
