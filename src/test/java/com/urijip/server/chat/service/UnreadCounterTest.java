package com.urijip.server.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import com.urijip.server.family.dto.response.MemberReadState;
import org.junit.jupiter.api.Test;

class UnreadCounterTest {

	private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 11, 9, 18, 0);

	private static final LocalDateTime BEFORE = SENT_AT.minusDays(1);

	@Test
	void countsMembersWhoHaveNotReadUpToTheMessage() {
		List<MemberReadState> members = List.of(
				new MemberReadState(1L, BEFORE, null),
				new MemberReadState(2L, BEFORE, 9L),
				new MemberReadState(3L, BEFORE, 10L),
				new MemberReadState(4L, BEFORE, 11L));

		assertThat(UnreadCounter.count(members, 10L, 99L, SENT_AT)).isEqualTo(2);
	}

	@Test
	void senderIsNotCounted() {
		List<MemberReadState> members = List.of(new MemberReadState(1L, BEFORE, null),
				new MemberReadState(2L, BEFORE, null));

		assertThat(UnreadCounter.count(members, 10L, 1L, SENT_AT)).isEqualTo(1);
	}

	@Test
	void membersWhoJoinedAfterTheMessageAreNotCounted() {
		List<MemberReadState> members = List.of(new MemberReadState(1L, BEFORE, null),
				new MemberReadState(2L, SENT_AT.plusSeconds(1), null),
				new MemberReadState(3L, SENT_AT, null));

		assertThat(UnreadCounter.count(members, 10L, 99L, SENT_AT)).isEqualTo(2);
	}

	@Test
	void systemMessageWithoutSenderCountsEveryone() {
		List<MemberReadState> members = List.of(new MemberReadState(1L, BEFORE, null),
				new MemberReadState(2L, BEFORE, null));

		assertThat(UnreadCounter.count(members, 10L, null, SENT_AT)).isEqualTo(2);
	}

}
