package com.urijip.server.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.List;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class StompAuthChannelInterceptorTest {

	private final JwtProvider jwtProvider = mock(JwtProvider.class);

	private final FamilyMembershipChecker familyMembershipChecker = mock(FamilyMembershipChecker.class);

	private final StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtProvider,
			familyMembershipChecker);

	private final MessageChannel channel = mock(MessageChannel.class);

	@Test
	void connectWithValidTokenSetsUser() {
		given(jwtProvider.parseAccessToken("good")).willReturn(new AccessTokenClaims(1L, "USER"));
		StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
		accessor.addNativeHeader("Authorization", "Bearer good");

		interceptor.preSend(message(accessor), channel);

		assertThat(accessor.getUser()).isInstanceOfSatisfying(UsernamePasswordAuthenticationToken.class, user -> {
			assertThat(user.getPrincipal()).isEqualTo(1L);
			assertThat(user.getName()).isEqualTo("1");
			assertThat(user.getAuthorities()).containsExactly(new SimpleGrantedAuthority("ROLE_USER"));
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "good", "Basic good"})
	void connectWithoutBearerTokenThrowsInvalidToken(String header) {
		StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
		if (!header.isEmpty()) {
			accessor.addNativeHeader("Authorization", header);
		}

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	@Test
	void connectWithExpiredTokenPassesErrorThrough() {
		given(jwtProvider.parseAccessToken("old")).willThrow(new BusinessException(ErrorCode.TOKEN_EXPIRED));
		StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
		accessor.addNativeHeader("Authorization", "Bearer old");

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.TOKEN_EXPIRED);
	}

	@Test
	void subscribeToOwnFamilyChecksMembership() {
		StompHeaderAccessor accessor = subscribe(1L, "/sub/family/3/location");

		interceptor.preSend(message(accessor), channel);

		then(familyMembershipChecker).should().validateMember(3L, 1L);
	}

	@Test
	void subscribeToOtherFamilyThrowsFamilyNotMember() {
		willThrow(new BusinessException(ErrorCode.FAMILY_NOT_MEMBER))
				.given(familyMembershipChecker).validateMember(3L, 9L);

		assertThatThrownBy(() -> interceptor.preSend(message(subscribe(9L, "/sub/family/3/chat")), channel))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/sub/users", "/sub/family/abc/location", "/sub/family/3", "/sub/family/3/location/x",
			"/pub/location"})
	void subscribeToOtherDestinationThrowsAccessDenied(String destination) {
		assertThatThrownBy(() -> interceptor.preSend(message(subscribe(1L, destination)), channel))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
		then(familyMembershipChecker).should(never()).validateMember(any(), any());
	}

	@Test
	void subscribeWithoutUserThrowsInvalidToken() {
		StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
		accessor.setDestination("/sub/family/3/location");

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
	}

	private StompHeaderAccessor subscribe(Long userId, String destination) {
		StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
		accessor.setDestination(destination);
		accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))));
		return accessor;
	}

	private StompHeaderAccessor accessor(StompCommand command) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
		accessor.setLeaveMutable(true);
		return accessor;
	}

	private Message<byte[]> message(StompHeaderAccessor accessor) {
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

}
