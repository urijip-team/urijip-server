package com.urijip.server.notice.service;

import com.urijip.server.global.response.PageResponse;
import com.urijip.server.notice.dto.request.NoticeSearchRequest;
import com.urijip.server.notice.dto.response.NoticeResponse;
import com.urijip.server.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeService {

	private final NoticeRepository noticeRepository;

	// 공지에는 작성 시각이 없어 id가 큰 것(나중에 쓴 것)부터 보여 준다.
	@Transactional(readOnly = true)
	public PageResponse<NoticeResponse> getNotices(NoticeSearchRequest request) {
		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(),
				Sort.by(Sort.Direction.DESC, "id"));
		return PageResponse.from(noticeRepository.findAll(pageable).map(NoticeResponse::from));
	}

}
