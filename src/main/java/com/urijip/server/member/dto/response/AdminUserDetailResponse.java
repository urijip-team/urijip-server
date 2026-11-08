package com.urijip.server.member.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.entity.UserStatus;
import com.urijip.server.report.dto.response.ReportHistoryResponse;

// 관리자용 회원 상세. 위치·채팅·계좌 정보는 넣지 않는다(공통 정책).
// reportCount는 이 회원과 그 회원이 올린 사진이 받은 신고 수, reports는 그중 최근 10건이다.
public record AdminUserDetailResponse(Long id, String email, String name, String profileImageUrl,
		LocalDate birthday, String statusMessage, UserStatus status, OffsetDateTime createdAt, Family family,
		long reportCount, List<ReportHistoryResponse.Item> reports) {

	public static AdminUserDetailResponse of(User user, FamilyResponse family, ReportHistoryResponse reports) {
		return new AdminUserDetailResponse(user.getId(), user.getEmail(), user.getName(), user.getProfileImageUrl(),
				user.getBirthday(), user.getStatusMessage(), user.getStatus(), SeoulTime.toOffset(user.getCreatedAt()),
				family == null ? null : new Family(family.id(), family.name()), reports.count(), reports.reports());
	}

	public record Family(Long id, String name) {
	}

}
