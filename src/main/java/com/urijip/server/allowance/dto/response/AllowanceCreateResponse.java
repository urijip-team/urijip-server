package com.urijip.server.allowance.dto.response;

import com.urijip.server.allowance.entity.AllowanceRequest;
import com.urijip.server.allowance.entity.AllowanceStatus;

public record AllowanceCreateResponse(Long requestId, AllowanceStatus status) {

	public static AllowanceCreateResponse from(AllowanceRequest request) {
		return new AllowanceCreateResponse(request.getId(), request.getStatus());
	}

}
