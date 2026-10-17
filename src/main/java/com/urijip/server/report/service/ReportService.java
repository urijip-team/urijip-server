package com.urijip.server.report.service;

import com.urijip.server.member.entity.User;
import com.urijip.server.member.service.UserService;
import com.urijip.server.photo.service.PhotoService;
import com.urijip.server.report.dto.request.ReportCreateRequest;
import com.urijip.server.report.dto.response.ReportResponse;
import com.urijip.server.report.entity.Report;
import com.urijip.server.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

	private final ReportRepository reportRepository;
	private final UserService userService;
	private final PhotoService photoService;

	@Transactional
	public ReportResponse create(Long reporterId, ReportCreateRequest request) {
		User reporter = userService.getUser(reporterId);
		checkTarget(reporterId, request);

		Report report = reportRepository.save(
				new Report(reporter, request.targetType(), request.targetId(), request.reason()));
		return ReportResponse.from(report);
	}

	// 사진은 가족 멤버만 볼 수 있으므로, 볼 수 있는 사진만 신고할 수 있다.
	private void checkTarget(Long reporterId, ReportCreateRequest request) {
		switch (request.targetType()) {
			case USER -> userService.getUser(request.targetId());
			case PHOTO -> photoService.getAccessiblePhoto(request.targetId(), reporterId);
		}
	}

}
