package com.urijip.server.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.chat.dto.request.ChatMessagePageRequest;
import com.urijip.server.chat.dto.request.ChatSendRequest;
import com.urijip.server.chat.dto.response.ChatMessageResponse;
import com.urijip.server.chat.entity.ChatMessage;
import com.urijip.server.chat.entity.MessageType;
import com.urijip.server.chat.repository.ChatMessageRepository;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.request.NicknameUpdateRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.response.CursorResponse;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RecordApplicationEvents
class ChatServiceTest {

	@Autowired
	private ChatService chatService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ApplicationEvents events;

	@Autowired
	private EntityManager entityManager;

	private User mom;

	private User kid;

	private User stranger;

	private Long familyId;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		kid = saveUser("kid@example.com", "김첫째");
		stranger = saveUser("stranger@example.com", "남");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("우리집"));
		familyService.join(kid.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyId = family.id();
	}

	@Test
	void sendSavesTextMessage() {
		ChatMessageResponse response = chatService.send(kid.getId(), new ChatSendRequest(familyId, "저녁 먹었어?"));

		ChatMessage saved = chatMessageRepository.findById(response.messageId()).orElseThrow();
		assertThat(saved.getType()).isEqualTo(MessageType.TEXT);
		assertThat(saved.getSender().getId()).isEqualTo(kid.getId());
		assertThat(saved.getFamily().getId()).isEqualTo(familyId);
		assertThat(response).extracting(ChatMessageResponse::type, ChatMessageResponse::senderId,
				ChatMessageResponse::senderName, ChatMessageResponse::content)
				.containsExactly(MessageType.TEXT, kid.getId(), "김첫째", "저녁 먹었어?");
		assertThat(response.createdAt().getOffset().getId()).isEqualTo("+09:00");
	}

	@Test
	void sendUsesFamilyNicknameAsSenderName() {
		familyService.changeMyNickname(familyId, kid.getId(), new NicknameUpdateRequest("첫째"));

		ChatMessageResponse response = chatService.send(kid.getId(), new ChatSendRequest(familyId, "응"));

		assertThat(response.senderName()).isEqualTo("첫째");
	}

	@Test
	void sendPublishesEventForFamily() {
		ChatMessageResponse response = chatService.send(kid.getId(), new ChatSendRequest(familyId, "저녁 먹었어?"));

		assertThat(events.stream(ChatMessageSentEvent.class))
				.containsExactly(new ChatMessageSentEvent(familyId, response));
	}

	@Test
	void sendToOtherFamilyThrowsFamilyNotMember() {
		assertThatThrownBy(() -> chatService.send(stranger.getId(), new ChatSendRequest(familyId, "안녕")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
		assertThat(chatMessageRepository.findAll()).isEmpty();
		assertThat(events.stream(ChatMessageSentEvent.class)).isEmpty();
	}

	@Test
	void getMessagesWithoutCursorReturnsLatestPage() {
		List<Long> ids = sendMany(35);

		CursorResponse<ChatMessageResponse> page = chatService.getMessages(familyId, mom.getId(),
				new ChatMessagePageRequest(null, null));

		assertThat(page.content()).hasSize(30);
		assertThat(page.content().getFirst().messageId()).isEqualTo(ids.getLast());
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextCursor()).isEqualTo(ids.get(5));
	}

	@Test
	void getMessagesWithCursorReturnsOlderMessagesUntilEnd() {
		List<Long> ids = sendMany(35);

		CursorResponse<ChatMessageResponse> page = chatService.getMessages(familyId, mom.getId(),
				new ChatMessagePageRequest(ids.get(5), 30));

		assertThat(page.content()).extracting(ChatMessageResponse::messageId)
				.containsExactly(ids.get(4), ids.get(3), ids.get(2), ids.get(1), ids.get(0));
		assertThat(page.hasNext()).isFalse();
		assertThat(page.nextCursor()).isNull();
	}

	@Test
	void getMessagesWithExactlySizeMessagesHasNoNext() {
		sendMany(3);

		CursorResponse<ChatMessageResponse> page = chatService.getMessages(familyId, mom.getId(),
				new ChatMessagePageRequest(null, 3));

		assertThat(page.content()).hasSize(3);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.nextCursor()).isNull();
	}

	@Test
	void getMessagesClampsSize() {
		sendMany(3);

		assertThat(chatService.getMessages(familyId, mom.getId(), new ChatMessagePageRequest(null, 0)).content())
				.hasSize(1);
		assertThat(new ChatMessagePageRequest(null, 1000).sizeOrDefault()).isEqualTo(100);
	}

	@Test
	void getMessagesOfEmptyRoomReturnsEmptyPage() {
		CursorResponse<ChatMessageResponse> page = chatService.getMessages(familyId, mom.getId(),
				new ChatMessagePageRequest(null, null));

		assertThat(page.content()).isEmpty();
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void getMessagesExcludesOtherFamily() {
		Long otherFamilyId = familyService.create(stranger.getId(), new FamilyCreateRequest("남의 집")).id();
		chatService.send(stranger.getId(), new ChatSendRequest(otherFamilyId, "남의 집 메시지"));
		chatService.send(mom.getId(), new ChatSendRequest(familyId, "우리 집 메시지"));

		assertThat(chatService.getMessages(familyId, mom.getId(), new ChatMessagePageRequest(null, null)).content())
				.extracting(ChatMessageResponse::content).containsExactly("우리 집 메시지");
	}

	@Test
	void getMessagesOfOtherFamilyThrowsFamilyNotMember() {
		assertThatThrownBy(() -> chatService.getMessages(familyId, stranger.getId(),
				new ChatMessagePageRequest(null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.FAMILY_NOT_MEMBER);
	}

	@Test
	void messageOfMemberWhoLeftShowsUserName() {
		familyService.changeMyNickname(familyId, kid.getId(), new NicknameUpdateRequest("첫째"));
		chatService.send(kid.getId(), new ChatSendRequest(familyId, "다녀올게"));
		familyService.removeMember(familyId, kid.getId(), kid.getId());

		ChatMessageResponse message = chatService.getMessages(familyId, mom.getId(),
				new ChatMessagePageRequest(null, null)).content().getFirst();

		assertThat(message.senderId()).isEqualTo(kid.getId());
		assertThat(message.senderName()).isEqualTo("김첫째");
	}

	@Test
	void deletingFamilyDeletesMessages() {
		User solo = saveUser("solo@example.com", "혼자");
		Long soloFamilyId = familyService.create(solo.getId(), new FamilyCreateRequest("혼자 집")).id();
		chatService.send(solo.getId(), new ChatSendRequest(soloFamilyId, "혼잣말"));
		chatService.send(mom.getId(), new ChatSendRequest(familyId, "남는 메시지"));
		// 실제로는 메시지 저장과 가족 삭제가 다른 트랜잭션이다. 벌크 삭제는 영속성 컨텍스트를 거치지 않으므로 비워 둔다.
		entityManager.flush();
		entityManager.clear();

		familyService.removeMember(soloFamilyId, solo.getId(), solo.getId());

		assertThat(chatMessageRepository.findAll()).extracting(ChatMessage::getContent)
				.containsExactly("남는 메시지");
	}

	private List<Long> sendMany(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(i -> chatService.send(mom.getId(), new ChatSendRequest(familyId, "메시지 " + i)).messageId())
				.toList();
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
