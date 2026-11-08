package com.urijip.server.family.dto.response;

import java.time.LocalDateTime;

import com.urijip.server.family.entity.FamilyMember;

// 채팅 안 읽은 사람 수를 셀 때 쓴다. 들어오기 전 메시지는 그 멤버가 읽어야 할 메시지가 아니다.
public record MemberReadState(Long userId, LocalDateTime joinedAt, Long lastReadMessageId) {

	public static MemberReadState from(FamilyMember member) {
		return new MemberReadState(member.getUser().getId(), member.getJoinedAt(), member.getLastReadMessageId());
	}

}
