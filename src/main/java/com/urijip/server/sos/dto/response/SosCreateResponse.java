package com.urijip.server.sos.dto.response;

import java.time.OffsetDateTime;

import com.urijip.server.global.time.SeoulTime;
import com.urijip.server.sos.entity.SosAlert;

public record SosCreateResponse(Long sosId, OffsetDateTime createdAt) {

	public static SosCreateResponse from(SosAlert alert) {
		return new SosCreateResponse(alert.getId(), SeoulTime.toOffset(alert.getCreatedAt()));
	}

}
