package com.urijip.server.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import com.urijip.server.global.push.PushMessage;
import com.urijip.server.global.push.PushType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class NotificationServiceTest {

	private static final PushMessage MESSAGE = PushMessage.of(PushType.NOTICE, "공지", "점검 안내");

	private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

	private final NotificationService notificationService = new NotificationService(eventPublisher);

	@Test
	void sendPublishesEventWithDistinctReceivers() {
		notificationService.send(List.of(1L, 2L, 1L), MESSAGE);

		ArgumentCaptor<PushRequestedEvent> event = ArgumentCaptor.forClass(PushRequestedEvent.class);
		verify(eventPublisher).publishEvent(event.capture());
		assertThat(event.getValue().userIds()).isEqualTo(Set.of(1L, 2L));
		assertThat(event.getValue().message()).isEqualTo(MESSAGE);
	}

	@Test
	void sendToNobodyPublishesNothing() {
		notificationService.send(List.of(), MESSAGE);

		verify(eventPublisher, never()).publishEvent(any(Object.class));
	}

	@Test
	void sendToAllPublishesBroadcastEvent() {
		notificationService.sendToAll(MESSAGE);

		verify(eventPublisher).publishEvent(new BroadcastPushRequestedEvent(MESSAGE));
	}

}
