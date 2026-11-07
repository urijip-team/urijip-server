package com.urijip.server.notice.controller;

import com.urijip.server.global.response.ApiResponse;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.notice.dto.request.NoticeSearchRequest;
import com.urijip.server.notice.dto.response.NoticeResponse;
import com.urijip.server.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

	private final NoticeService noticeService;

	@GetMapping
	public ApiResponse<PageResponse<NoticeResponse>> getNotices(@ModelAttribute NoticeSearchRequest request) {
		return ApiResponse.success(noticeService.getNotices(request));
	}

}
