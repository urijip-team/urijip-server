package com.urijip.server.global.security;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT 프레임의 {@code Authorization} 헤더로 Access 토큰을 검사하고, 그 회원을 세션 사용자로 정한다.
 * SUBSCRIBE는 {@code /sub/family/{familyId}/...}만 허용하고 요청자가 그 가족의 멤버인지 확인한다.
 * 여기서 던진 예외는 {@link StompErrorHandler}가 ERROR 프레임으로 바꾸고 연결을 끊는다.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final Pattern FAMILY_DESTINATION = Pattern.compile("^/sub/family/(\\d+)/[a-z]+$");

	private final JwtProvider jwtProvider;
	private final FamilyMembershipChecker familyMembershipChecker;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) {
			return message;
		}
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
		} else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			validateSubscription(userIdOf(accessor.getUser()), accessor.getDestination());
		}
		return message;
	}

	private Principal authenticate(String header) {
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
		AccessTokenClaims claims = jwtProvider.parseAccessToken(header.substring(BEARER_PREFIX.length()));
		return new UsernamePasswordAuthenticationToken(claims.userId(), null,
				List.of(new SimpleGrantedAuthority("ROLE_" + claims.role())));
	}

	private void validateSubscription(Long userId, String destination) {
		Matcher matcher = FAMILY_DESTINATION.matcher(destination == null ? "" : destination);
		if (!matcher.matches()) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED);
		}
		familyMembershipChecker.validateMember(Long.valueOf(matcher.group(1)), userId);
	}

	private Long userIdOf(Principal user) {
		if (!(user instanceof UsernamePasswordAuthenticationToken authentication)) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}
		return (Long) authentication.getPrincipal();
	}

}
