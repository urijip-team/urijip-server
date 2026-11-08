package com.urijip.server.notification.service;

import com.urijip.server.global.push.PushMessage;

// 활동 중인 모든 회원에게 보내는 푸시 요청. 공지 전체 푸시에 쓴다.
public record BroadcastPushRequestedEvent(PushMessage message) {
}
