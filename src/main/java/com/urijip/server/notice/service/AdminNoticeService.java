package com.urijip.server.notice.service;

import java.util.Map;

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
	private final NotificationService notificationService;

	// push가 true면 활동 중인 모든 회원에게 공지 제목으로 푸시한다.
	@Transactional
	public AdminNoticeResponse create(Long adminId, AdminNoticeCreateRequest request) {
		Notice notice = noticeRepository.save(new Notice(userService.getUser(adminId), request.title(),
				request.content()));
		if (request.pushRequested()) {
			notificationService.sendToAll(new PushMessage(PushType.NOTICE, "공지사항", notice.getTitle(),
					Map.of("noticeId", notice.getId().toString())));
			notice.markPushed();
		}
		return AdminNoticeResponse.from(notice);
	}

	@Transactional
	public AdminNoticeResponse update(Long noticeId, AdminNoticeUpdateRequest request) {
		Notice notice = getNotice(noticeId);
		notice.update(request.title(), request.content());
		return AdminNoticeResponse.from(notice);
	}

	@Transactional
	public void delete(Long noticeId) {
		noticeRepository.delete(getNotice(noticeId));
	}

	private Notice getNotice(Long noticeId) {
		return noticeRepository.findById(noticeId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
	}

}
