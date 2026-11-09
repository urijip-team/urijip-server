package com.urijip.server.chat.service;

import java.time.LocalDateTime;
import java.util.List;

import com.urijip.server.family.dto.response.MemberReadState;

/**
 * 메시지별 안 읽은 사람 수. 보낸 사람과, 메시지가 온 뒤에 가족에 들어온 멤버는 세지 않는다.
 * 마지막으로 읽은 메시지 id가 이 메시지보다 작거나 없으면 안 읽은 것이다.
 */
final class UnreadCounter {

	private UnreadCounter() {
	}

	static int count(List<MemberReadState> members, Long messageId, Long senderId, LocalDateTime createdAt) {
		return (int) members.stream()
				.filter(member -> !member.userId().equals(senderId))
				.filter(member -> !member.joinedAt().isAfter(createdAt))
				.filter(member -> member.lastReadMessageId() == null || member.lastReadMessageId() < messageId)
				.count();
	}

}
