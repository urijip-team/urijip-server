package com.urijip.server.sos.service;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;

import com.urijip.server.sos.dto.response.SosResponse;
import com.urijip.server.sos.entity.SosStatus;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class SosBroadcasterTest {

	@Test
	void sendsMessageToFamilySosTopic() {
		SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
		SosResponse message = new SosResponse(12L, 2L, "첫째", 35.1595, 126.8526, SosStatus.ACTIVE,
				OffsetDateTime.parse("2026-10-02T14:00:00+09:00"), null, null);

		new SosBroadcaster(template).onSosChanged(new SosChangedEvent(3L, message));

		then(template).should().convertAndSend("/sub/family/3/sos", (Object) message);
	}

}
