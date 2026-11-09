package com.urijip.server.notification.dto.request;

// 보낸 항목만 바꾼다.
public record NotificationSettingUpdateRequest(Boolean chat, Boolean schedule, Boolean allowance, Boolean notice,
		Boolean place) {
}
