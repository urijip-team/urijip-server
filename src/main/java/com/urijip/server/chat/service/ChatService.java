package com.urijip.server.chat.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.urijip.server.chat.dto.request.ChatMessagePageRequest;
import com.urijip.server.chat.dto.request.ChatSendRequest;
import com.urijip.server.chat.dto.response.ChatMessageResponse;
import com.urijip.server.chat.entity.ChatMessage;
import com.urijip.server.chat.repository.ChatMessageRepository;
import com.urijip.server.family.dto.response.FamilyMemberResponse;
import com.urijip.server.family.service.FamilyDeletedEvent;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.push.PushMessage;
import com.urijip.server.global.push.PushType;
import com.urijip.server.global.response.CursorResponse;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.service.UserService;
import com.urijip.server.notification.service.NotificationService;
import com.urijip.server.notification.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

	private final ChatMessageRepository chatMessageRepository;
	private final FamilyService familyService;
	private final UserService userService;
	private final NotificationService notificationService;
	private final PresenceService presenceService;
	private final ApplicationEventPublisher eventPublisher;

	// STOMP /pub/chat. 본문의 가족에 속하지 않으면 FAMILY_NOT_MEMBER. 저장한 뒤 가족에게 실시간으로 보내고,
	// 앱이 꺼져 있는(WebSocket에 연결되지 않은) 가족에게는 푸시한다.
	@Transactional
	public ChatMessageResponse send(Long userId, ChatSendRequest request) {
		Map<Long, String> names = displayNames(request.familyId(), userId);
		User sender = userService.getUser(userId);

		ChatMessage message = chatMessageRepository.save(
				ChatMessage.text(familyService.getFamily(request.familyId()), sender, request.content()));

		ChatMessageResponse response = toResponse(message, names);
		eventPublisher.publishEvent(new ChatMessageSentEvent(request.familyId(), response));
		pushToOffline(response, request.familyId(), userId, names);
		return response;
	}

	// 서버가 이벤트를 알리는 메시지. 보낸 사람 없이 저장하고 가족에게 실시간으로 보낸다.
	@Transactional
	public void sendSystemMessage(Long familyId, String content) {
		ChatMessage message = chatMessageRepository.save(
				ChatMessage.system(familyService.getFamily(familyId), content));
		eventPublisher.publishEvent(new ChatMessageSentEvent(familyId, ChatMessageResponse.of(message, null)));
	}

	// 최신순. size개보다 하나 더 읽어 다음 페이지가 있는지 판단한다.
	@Transactional(readOnly = true)
	public CursorResponse<ChatMessageResponse> getMessages(Long familyId, Long userId,
			ChatMessagePageRequest request) {
		Map<Long, String> names = displayNames(familyId, userId);
		int size = request.sizeOrDefault();
		Limit limit = Limit.of(size + 1);

		List<ChatMessage> messages = request.cursor() == null
				? chatMessageRepository.findByFamilyIdOrderByIdDesc(familyId, limit)
				: chatMessageRepository.findByFamilyIdAndIdLessThanOrderByIdDesc(familyId, request.cursor(), limit);

		boolean hasNext = messages.size() > size;
		List<ChatMessageResponse> content = messages.stream()
				.limit(size)
				.map(message -> toResponse(message, names))
				.toList();
		Long nextCursor = hasNext ? content.getLast().messageId() : null;
		return new CursorResponse<>(content, nextCursor, hasNext);
	}

	@EventListener
	public void onFamilyDeleted(FamilyDeletedEvent event) {
		chatMessageRepository.deleteByFamilyId(event.familyId());
	}

	private void pushToOffline(ChatMessageResponse message, Long familyId, Long senderId, Map<Long, String> names) {
		Set<Long> others = names.keySet().stream()
				.filter(memberId -> !memberId.equals(senderId))
				.collect(Collectors.toSet());
		notificationService.send(presenceService.filterOffline(others), new PushMessage(PushType.CHAT,
				message.senderName(), message.content(),
				Map.of("familyId", familyId.toString(), "messageId", message.messageId().toString())));
	}

	// 보낸 사람 이름은 가족 안 호칭을 먼저 쓴다. 메시지를 보낸 뒤 가족을 나간 회원은 회원 이름을 쓴다.
	private Map<Long, String> displayNames(Long familyId, Long userId) {
		return familyService.getMembers(familyId, userId).stream()
				.collect(Collectors.toMap(FamilyMemberResponse::userId,
						member -> member.nickname() == null ? member.name() : member.nickname()));
	}

	private ChatMessageResponse toResponse(ChatMessage message, Map<Long, String> names) {
		User sender = message.getSender();
		String senderName = sender == null ? null : names.getOrDefault(sender.getId(), sender.getName());
		return ChatMessageResponse.of(message, senderName);
	}

}
