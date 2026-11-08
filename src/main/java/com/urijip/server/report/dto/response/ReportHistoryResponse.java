package com.urijip.server.report.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;

// 한 회원이 받은 신고(회원 자신과 올린 사진). count는 전체 건수, reports는 최근 몇 건이다.
public record ReportHistoryResponse(long count, List<Item> reports) {

	public static ReportHistoryResponse of(long count, List<Report> reports) {
		return new ReportHistoryResponse(count, reports.stream().map(Item::from).toList());
	}

	public record Item(Long id, ReportTargetType targetType, Long targetId, String reason, ReportStatus status,
			OffsetDateTime createdAt) {

		static Item from(Report report) {
			return new Item(report.getId(), report.getTargetType(), report.getTargetId(), report.getReason(),
					report.getStatus(), SeoulTime.toOffset(report.getCreatedAt()));
		}

	}

}
