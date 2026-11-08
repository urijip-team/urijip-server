package com.urijip.server.allowance.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.allowance.entity.AllowanceStatus;
import com.urijip.server.member.dto.response.PaymentInfoResponse;

// AllowanceResponse에 요청자의 송금 정보를 더했다. 요청자가 송금 정보를 등록하지 않았으면 null이다.
// 전체 계좌번호는 GET /api/users/{requesterId}/payment-info/account로 받는다.
public record AllowanceDetailResponse(Long id, Long familyId, AllowanceResponse.Person requester,
		AllowanceResponse.Person receiver, int amount, String reason, AllowanceStatus status, OffsetDateTime createdAt,
		OffsetDateTime completedAt, PaymentInfoResponse requesterPaymentInfo) {

	public static AllowanceDetailResponse of(AllowanceResponse request, PaymentInfoResponse requesterPaymentInfo) {
		return new AllowanceDetailResponse(request.id(), request.familyId(), request.requester(), request.receiver(),
				request.amount(), request.reason(), request.status(), request.createdAt(), request.completedAt(),
				requesterPaymentInfo);
	}

}
