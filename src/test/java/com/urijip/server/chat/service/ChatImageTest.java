package com.urijip.server.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.urijip.server.LocalStackTestConfiguration;
import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.chat.dto.response.ChatMessageResponse;
import com.urijip.server.chat.entity.ChatMessage;
import com.urijip.server.chat.entity.MessageType;
import com.urijip.server.chat.repository.ChatMessageRepository;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.request.FamilyJoinRequest;
import com.urijip.server.family.service.FamilyService;
import com.urijip.server.global.exception.BusinessException;
import com.urijip.server.global.exception.ErrorCode;
import com.urijip.server.global.push.PushType;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import com.urijip.server.notification.service.PushRequestedEvent;
import com.urijip.server.photo.TestImages;
import com.urijip.server.photo.service.PhotoFilesDeletedEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

// 사진은 LocalStack S3에 실제로 올린다.
@SpringBootTest
@Import({TestcontainersConfiguration.class, LocalStackTestConfiguration.class})
@Transactional
@RecordApplicationEvents
class ChatImageTest {

	private static final String BUCKET = "urijip-local";

	@Autowired
	private ChatService chatService;

	@Autowired
	private FamilyService familyService;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private S3Client s3Client;

	@Autowired
	private ApplicationEvents events;

	@Autowired
	private EntityManager entityManager;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private User mom;

	private User kid;

	private User stranger;

	private Long familyId;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		kid = saveUser("kid@example.com", "김첫째");
		stranger = saveUser("stranger@example.com", "남");
		familyId = familyService.create(mom.getId(), new FamilyCreateRequest("우리집")).id();
		familyService.join(kid.getId(), new FamilyJoinRequest(
				familyService.getMyFamily(mom.getId()).inviteCode()));
	}

	@Test
	void sendImageStoresOriginalInS3AndSavesKey() throws Exception {
		MockMultipartFile file = TestImages.jpeg(800, 600);

		ChatMessageResponse response = chatService.sendImage(familyId, kid.getId(), file);

		assertThat(response.type()).isEqualTo(MessageType.IMAGE);
		assertThat(response.senderId()).isEqualTo(kid.getId());
		assertThat(response.unreadCount()).isEqualTo(1);
		assertThat(download(response.content())).isEqualTo(file.getBytes());
		ChatMessage saved = chatMessageRepository.findById(response.messageId()).orElseThrow();
		assertThat(saved.getContent()).startsWith("families/" + familyId + "/chat/").endsWith(".jpg");
		assertThat(objectKeys()).containsExactly(saved.getContent());
	}

	@Test
	void sendImageIsDeliveredAndPushedAsPhotoMessage() {
		ChatMessageResponse response = chatService.sendImage(familyId, kid.getId(), TestImages.jpeg(10, 10));

		assertThat(events.stream(ChatMessageSentEvent.class)).singleElement()
				.satisfies(event -> assertThat(event.message()).isEqualTo(response));
		assertThat(events.stream(PushRequestedEvent.class)).singleElement().satisfies(event -> {
			assertThat(event.message().type()).isEqualTo(PushType.CHAT);
			assertThat(event.message().body()).isEqualTo("사진을 보냈어요.");
		});
	}

	@Test
	void sendImageByStrangerUploadsNothing() {
		assertThatThrownBy(() -> chatService.sendImage(familyId, stranger.getId(), TestImages.jpeg(10, 10)))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FAMILY_NOT_MEMBER));
		assertThat(objectKeys()).isEmpty();
	}

	@Test
	void sendNonImageFileFails() {
		assertThatThrownBy(() -> chatService.sendImage(familyId, kid.getId(),
				TestImages.file("not an image".getBytes())))
				.isInstanceOf(BusinessException.class);
		assertThat(objectKeys()).isEmpty();
		assertThat(chatMessageRepository.count()).isZero();
	}

	@Test
	void sendEmptyImageFails() {
		assertThatThrownBy(() -> chatService.sendImage(familyId, kid.getId(), null))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
	}

	@Test
	void deletingFamilyRequestsDeletionOfChatImages() {
		ChatMessageResponse response = chatService.sendImage(familyId, kid.getId(), TestImages.jpeg(10, 10));
		String key = chatMessageRepository.findById(response.messageId()).orElseThrow().getContent();
		// 메시지는 벌크 삭제되므로, 읽어 둔 엔티티가 남지 않게 트랜잭션 경계를 흉내 낸다.
		entityManager.flush();
		entityManager.clear();

		familyService.leaveFamily(kid.getId());
		familyService.leaveFamily(mom.getId());

		assertThat(chatMessageRepository.count()).isZero();
		// 앨범 사진 쪽도 (빈) 삭제 요청을 보내므로 채팅 사진이 든 요청을 찾는다.
		assertThat(events.stream(PhotoFilesDeletedEvent.class))
				.anySatisfy(event -> assertThat(event.keys()).containsExactly(key));
	}

	private List<String> objectKeys() {
		return s3Client.listObjectsV2(request -> request.bucket(BUCKET).prefix("families/%d/chat/".formatted(familyId)))
				.contents().stream()
				.map(S3Object::key)
				.toList();
	}

	private byte[] download(String url) throws IOException, InterruptedException {
		HttpResponse<byte[]> response = httpClient.send(HttpRequest.newBuilder(URI.create(url)).build(),
				HttpResponse.BodyHandlers.ofByteArray());
		assertThat(response.statusCode()).isEqualTo(200);
		return response.body();
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.builder().email(email).password("encoded-password").name(name).build());
	}

}
