package com.urijip.server.report.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.urijip.server.global.response.PageResponse;
import com.urijip.server.member.dto.response.AdminUserResponse;
import com.urijip.server.member.service.UserService;
import com.urijip.server.photo.dto.response.PhotoResponse;
import com.urijip.server.photo.service.PhotoService;
import com.urijip.server.report.dto.request.AdminReportSearchRequest;
import com.urijip.server.report.dto.response.AdminReportResponse;
import com.urijip.server.report.entity.Report;
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

	private List<Long> targetIds(Page<Report> reports, ReportTargetType targetType) {
		return reports.stream()
				.filter(report -> report.getTargetType() == targetType)
				.map(Report::getTargetId)
				.distinct()
				.toList();
	}

}
