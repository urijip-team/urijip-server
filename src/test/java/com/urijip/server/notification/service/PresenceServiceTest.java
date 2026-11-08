package com.urijip.server.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

class PresenceServiceTest {

	private final SimpUserRegistry registry = mock(SimpUserRegistry.class);

	private final PresenceService presenceService = new PresenceService(registry);

	@Test
	void filterOfflineKeepsOnlyUsersWithoutConnection() {
		given(registry.getUser("1")).willReturn(mock(SimpUser.class));

		assertThat(presenceService.filterOffline(List.of(1L, 2L, 3L))).isEqualTo(Set.of(2L, 3L));
	}

	@Test
	void filterOfflineOfNobodyIsEmpty() {
		assertThat(presenceService.filterOffline(List.of())).isEmpty();
	}

}
