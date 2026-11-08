package com.urijip.server.family.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FamilyMemberTest {

	private final FamilyMember member = FamilyMember.builder().role(FamilyRole.MEMBER).build();

	@Test
	void newMemberHasReadNothing() {
		assertThat(member.getLastReadMessageId()).isNull();
	}

	@Test
	void markReadMovesForwardOnly() {
		assertThat(member.markRead(10L)).isTrue();
		assertThat(member.markRead(12L)).isTrue();

		assertThat(member.markRead(11L)).isFalse();
		assertThat(member.markRead(12L)).isFalse();
		assertThat(member.getLastReadMessageId()).isEqualTo(12L);
	}

}
