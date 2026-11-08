package com.urijip.server.global.push;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class LoggingPushSenderTest {

	@Test
	void sendOnlyLogsAndReportsNoInvalidTokens(CapturedOutput output) {
		LoggingPushSender sender = new LoggingPushSender();

		assertThat(sender.send(List.of("token-a", "token-b"), PushMessage.of(PushType.NOTICE, "공지", "점검")))
				.isEmpty();
		assertThat(output).contains("type=NOTICE").contains("tokens=2").doesNotContain("token-a");
	}

}
