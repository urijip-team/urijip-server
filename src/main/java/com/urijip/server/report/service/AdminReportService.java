package com.urijip.server.report.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.PageResponse;
import com.urijip.server.member.dto.response.AdminUserResponse;
import com.urijip.server.member.service.UserService;
import com.urijip.server.photo.dto.response.PhotoResponse;
import com.urijip.server.photo.service.PhotoService;
import com.urijip.server.report.dto.request.AdminReportHandleRequest;
import com.urijip.server.report.dto.request.AdminReportSearchRequest;
import com.urijip.server.report.dto.response.AdminReportResponse;
import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;
import com.urijip.server.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReportService {

	private final ReportRepository reportRepository;
	private final UserService userService;
	private final PhotoService photoService;

	// 신고 대상은 종류별로 한 번에 읽어 페이지 크기만큼 쿼리가 늘지 않게 한다.
	@Transactional(readOnly = true)
	public PageResponse<AdminReportResponse> search(AdminReportSearchRequest request) {
		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(),
				Sort.by(Sort.Direction.DESC, "id"));
		Page<Report> reports = reportRepository.search(request.status(), pageable);

		Map<Long, AdminUserResponse> users = userService.getUsers(targetIds(reports, ReportTargetType.USER)).stream()
				.map(AdminUserResponse::from)
				.collect(Collectors.toMap(AdminUserResponse::id, Function.identity()));
		Map<Long, PhotoResponse> photos = photoService.getPhotos(targetIds(reports, ReportTargetType.PHOTO)).stream()
				.collect(Collectors.toMap(PhotoResponse::id, Function.identity()));

		return PageResponse.from(reports.map(report -> switch (report.getTargetType()) {
			case USER -> AdminReportResponse.of(report, users.get(report.getTargetId()), null);
			case PHOTO -> AdminReportResponse.of(report, null, photos.get(report.getTargetId()));
		}));
	}

	// 처리를 기다리는 신고만 처리할 수 있다.
	@Transactional
	public AdminReportResponse handle(Long reportId, Long adminId, AdminReportHandleRequest request) {
		Report report = reportRepository.findById(reportId)
				.orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
		if (!report.isPending()) {
			throw new BusinessException(ErrorCode.REPORT_ALREADY_HANDLED);
		}

		switch (request.action()) {
			case REJECT -> report.handle(userService.getUser(adminId), ReportStatus.REJECTED);
		}

		return toResponse(report);
	}

	private AdminReportResponse toResponse(Report report) {
		return switch (report.getTargetType()) {
			case USER -> AdminReportResponse.of(report, userService.getUsers(List.of(report.getTargetId())).stream()
					.findFirst().map(AdminUserResponse::from).orElse(null), null);
			case PHOTO -> AdminReportResponse.of(report, null,
					photoService.getPhotos(List.of(report.getTargetId())).stream().findFirst().orElse(null));
		};
	}

	private List<Long> targetIds(Page<Report> reports, ReportTargetType targetType) {
		return reports.stream()
				.filter(report -> report.getTargetType() == targetType)
				.map(Report::getTargetId)
				.distinct()
				.toList();
	}

}
