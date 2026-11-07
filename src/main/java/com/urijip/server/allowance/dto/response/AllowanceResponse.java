package com.urijip.server.allowance.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.allowance.entity.AllowanceRequest;
import com.urijip.server.allowance.entity.AllowanceStatus;
import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.member.entity.User;

// 이름은 회원 이름이다(가족 호칭이 아니다). completedAt은 처리 전에는 null이다.
public record AllowanceResponse(Long id, Long familyId, Person requester, Person receiver, int amount, String reason,
		AllowanceStatus status, OffsetDateTime createdAt, OffsetDateTime completedAt) {

	public record Person(Long id, String name) {

		static Person from(User user) {
			return new Person(user.getId(), user.getName());
		}

	}

	public static AllowanceResponse from(AllowanceRequest request) {
		return new AllowanceResponse(request.getId(), request.getFamily().getId(), Person.from(request.getRequester()),
				Person.from(request.getReceiver()), request.getAmount(), request.getReason(), request.getStatus(),
				SeoulTime.toOffset(request.getCreatedAt()), SeoulTime.toOffset(request.getCompletedAt()));
	}

}
