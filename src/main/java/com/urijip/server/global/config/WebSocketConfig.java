package com.urijip.server.global.config;

import com.urijip.server.global.security.StompAuthChannelInterceptor;
import com.urijip.server.global.security.StompErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 설정. 앱은 {@code /ws}로 연결하고, {@code /pub/...}로 보내고 {@code /sub/...}를 구독한다.
 * 인증은 HTTP 핸드셰이크가 아닌 CONNECT 프레임에서 한다({@link StompAuthChannelInterceptor}).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private static final long HEARTBEAT_MILLIS = 10_000;

	private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
	private final StompErrorHandler stompErrorHandler;

	// Spring이 만드는 브로커용 스케줄러를 하트비트에 쓴다. 따로 TaskScheduler 빈을 만들면 Boot가 그것을
	// STOMP 메시지 처리 스레드로도 써서 메시지가 스레드 하나로 몰린다.
	private TaskScheduler messageBrokerTaskScheduler;

	@Autowired
	public void setMessageBrokerTaskScheduler(@Lazy TaskScheduler messageBrokerTaskScheduler) {
		this.messageBrokerTaskScheduler = messageBrokerTaskScheduler;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 앱은 브라우저가 아니라 Origin 제한이 의미가 없다. 인증은 CONNECT 토큰으로 한다.
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
		registry.setErrorHandler(stompErrorHandler);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/sub")
				.setHeartbeatValue(new long[] {HEARTBEAT_MILLIS, HEARTBEAT_MILLIS})
				.setTaskScheduler(messageBrokerTaskScheduler);
		registry.setApplicationDestinationPrefixes("/pub");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthChannelInterceptor);
	}

}
