package com.urijip.server.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.chat.entity.ChatMessage;
import com.urijip.server.chat.repository.ChatMessageRepository;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.security.JwtProvider;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

// 실제 서버를 띄우고 STOMP 클라이언트로 연결한다. 서버 쪽 트랜잭션이 커밋되므로 만든 행은 직접 지운다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ChatSocketControllerTest {

	private static final long TIMEOUT_SECONDS = 5;

	@LocalServerPort
	private int port;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final List<StompSession> sessions = new ArrayList<>();

	private WebSocketStompClient stompClient;

	private User mom;

	private User dad;

	private User stranger;

	private Long familyId;

	private Long strangerFamilyId;

	@BeforeEach
	void setUp() {
		stompClient = new WebSocketStompClient(new StandardWebSocketClient());
		stompClient.setMessageConverter(new CompositeMessageConverter(
				List.of(new ByteArrayMessageConverter(), new JacksonJsonMessageConverter())));

		mom = saveUser("chat-mom@example.com", "김엄마");
		dad = saveUser("chat-dad@example.com", "김아빠");
		stranger = saveUser("chat-stranger@example.com", "남");
		FamilyResponse family = familyService.create(mom.getId(), new FamilyCreateRequest("우리집"));
		familyService.join(dad.getId(), new FamilyJoinRequest(family.inviteCode()));
		familyId = family.id();
		strangerFamilyId = familyService.create(stranger.getId(), new FamilyCreateRequest("남의 집")).id();
	}

	@AfterEach
	void tearDown() {
		sessions.stream().filter(StompSession::isConnected).forEach(StompSession::disconnect);
		stompClient.stop();
		List<Long> userIds = List.of(mom.getId(), dad.getId(), stranger.getId());
		for (String sql : List.of("DELETE FROM chat_messages WHERE sender_id = ?",
				"DELETE FROM family_members WHERE user_id = ?", "DELETE FROM families WHERE owner_id = ?",
				"DELETE FROM users WHERE id = ?")) {
			userIds.forEach(id -> jdbcTemplate.update(sql, id));
		}
	}

	@Test
	void sentMessageIsSavedAndDeliveredToFamily() throws Exception {
		MessageCollector momInbox = subscribeChat(mom);
		StompSession dadSession = connect(token(dad), new LinkedBlockingQueue<>());

		Map<String, Object> message = momInbox.awaitWhileRepeating(
				() -> dadSession.send("/pub/chat", Map.of("familyId", familyId, "content", "저녁 먹었어?")));

		assertThat(message).containsEntry("type", "TEXT")
				.containsEntry("senderId", dad.getId().intValue())
				.containsEntry("senderName", "김아빠")
				.containsEntry("content", "저녁 먹었어?");
		assertThat(message.get("messageId")).isNotNull();
		assertThat((String) message.get("createdAt")).endsWith("+09:00");
		ChatMessage saved = chatMessageRepository.findById(((Number) message.get("messageId")).longValue())
				.orElseThrow();
		assertThat(saved.getContent()).isEqualTo("저녁 먹었어?");
	}

	@Test
	void subscribeToOtherFamilyChatIsRejected() throws Exception {
		BlockingQueue<StompHeaders> errors = new LinkedBlockingQueue<>();
		StompSession session = connect(token(mom), errors);

		session.subscribe("/sub/family/" + strangerFamilyId + "/chat", new MessageCollector());

		assertThat(errors.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getFirst("message"))
				.isEqualTo("FAMILY_NOT_MEMBER");
	}

	@Test
	void messageSentWithOtherFamilyIdIsIgnored() throws Exception {
		StompSession session = connect(token(stranger), new LinkedBlockingQueue<>());

		session.send("/pub/chat", Map.of("familyId", familyId, "content", "남의 집에 보냄"));
		session.send("/pub/chat", Map.of("familyId", strangerFamilyId, "content", "우리 집에 보냄"));

		assertThat(awaitContentsOf(stranger, "우리 집에 보냄")).containsExactly("우리 집에 보냄");
	}

	@Test
	void blankMessageIsIgnored() throws Exception {
		StompSession session = connect(token(dad), new LinkedBlockingQueue<>());

		session.send("/pub/chat", Map.of("familyId", familyId, "content", " "));
		session.send("/pub/chat", Map.of("familyId", familyId, "content", "보임"));

		assertThat(awaitContentsOf(dad, "보임")).containsExactly("보임");
	}

	private MessageCollector subscribeChat(User user) throws Exception {
		MessageCollector collector = new MessageCollector();
		connect(token(user), new LinkedBlockingQueue<>()).subscribe("/sub/family/" + familyId + "/chat", collector);
		return collector;
	}

	// 메시지는 서버의 여러 스레드에서 처리되어 순서가 보장되지 않는다. 정상 메시지가 저장될 때까지 기다린 뒤,
	// 잘못된 메시지가 늦게 저장되는 경우도 잡도록 잠시 더 기다렸다가 이 회원의 메시지를 모두 돌려준다.
	private List<String> awaitContentsOf(User user, String expected) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
		while (System.nanoTime() < deadline) {
			if (contentsOf(user).contains(expected)) {
				Thread.sleep(300);
				return contentsOf(user);
			}
			Thread.sleep(50);
		}
		throw new AssertionError("message was not saved");
	}

	private List<String> contentsOf(User user) {
		return jdbcTemplate.queryForList("SELECT content FROM chat_messages WHERE sender_id = ?", String.class,
				user.getId());
	}

	private StompSession connect(String accessToken, BlockingQueue<StompHeaders> errors)
			throws InterruptedException, ExecutionException {
		StompHeaders connectHeaders = new StompHeaders();
		if (accessToken != null) {
			connectHeaders.add("Authorization", "Bearer " + accessToken);
		}
		try {
			StompSession session = stompClient.connectAsync("ws://localhost:" + port + "/ws",
					new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
						@Override
						public Type getPayloadType(StompHeaders headers) {
							return byte[].class;
						}

						@Override
						public void handleFrame(StompHeaders headers, Object payload) {
							errors.add(headers);
						}
					}).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			sessions.add(session);
			return session;
		} catch (TimeoutException | ExecutionException e) {
			// CONNECT가 거절되면 연결이 끝나지 않는다. ERROR 프레임은 errors로 들어온다.
			return null;
		}
	}

	private String token(User user) {
		return jwtProvider.createAccessToken(user.getId(), "USER");
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

	private static final class MessageCollector implements StompFrameHandler {

		private final BlockingQueue<Map<String, Object>> messages = new LinkedBlockingQueue<>();

		@Override
		public Type getPayloadType(StompHeaders headers) {
			return Map.class;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void handleFrame(StompHeaders headers, Object payload) {
			messages.add((Map<String, Object>) payload);
		}

		// 구독이 서버에 등록되기 전에 보낸 메시지는 사라지므로, 받을 때까지 같은 동작을 되풀이한다.
		Map<String, Object> awaitWhileRepeating(Runnable action) throws InterruptedException {
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
			while (System.nanoTime() < deadline) {
				action.run();
				Map<String, Object> message = messages.poll(200, TimeUnit.MILLISECONDS);
				if (message != null) {
					return message;
				}
			}
			throw new AssertionError("no message received");
		}

	}

}
