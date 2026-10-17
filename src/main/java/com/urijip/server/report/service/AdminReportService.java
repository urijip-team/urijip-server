package com.urijip.server.report.service;

import com.urijip.server.global.response.PageResponse;
import com.urijip.server.report.dto.request.AdminReportSearchRequest;
import com.urijip.server.report.dto.response.AdminReportResponse;
import com.urijip.server.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReportService {

	private final ReportRepository reportRepository;

	@Transactional(readOnly = true)
	public PageResponse<AdminReportResponse> search(AdminReportSearchRequest request) {
		PageRequest pageable = PageRequest.of(request.pageOrDefault(), request.sizeOrDefault(),
				Sort.by(Sort.Direction.DESC, "id"));

		return PageResponse.from(reportRepository.search(request.status(), pageable)
				.map(AdminReportResponse::from));
	}

}
