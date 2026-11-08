package com.urijip.server.allowance.dto.response;

import java.util.List;

import com.urijip.server.allowance.entity.AllowanceStatus;

// totalAmount는 그 달 요청 중 보냄(SENT)으로 처리된 금액의 합이다. 대기·거절은 더하지 않는다.
public record AllowanceMonthlyResponse(List<AllowanceResponse> requests, long totalAmount) {

	public static AllowanceMonthlyResponse from(List<AllowanceResponse> requests) {
		long total = requests.stream()
				.filter(request -> request.status() == AllowanceStatus.SENT)
				.mapToLong(AllowanceResponse::amount)
				.sum();
		return new AllowanceMonthlyResponse(requests, total);
	}

}
