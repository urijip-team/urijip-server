package com.urijip.server.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.chat.entity.ChatMessage;
import com.urijip.server.chat.entity.MessageType;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class ChatMessageRepositoryTest {

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private Family kims;

	private Family parks;

	@BeforeEach
	void setUp() {
		mom = userRepository.saveAndFlush(User.builder()
				.email("mom@example.com")
				.password("encoded-password")
				.name("김엄마")
				.build());
		kims = familyRepository.saveAndFlush(Family.builder().name("김씨네").inviteCode("A3K9P2").owner(mom).build());
		parks = familyRepository.saveAndFlush(Family.builder().name("박씨네").inviteCode("B7Q4M1").owner(mom).build());
	}

	@Test
	void savedMessageGetsTypeAndCreatedAt() {
		ChatMessage message = chatMessageRepository.saveAndFlush(ChatMessage.text(kims, mom, "저녁 먹었어?"));

		assertThat(message.getType()).isEqualTo(MessageType.TEXT);
		assertThat(message.getSender()).isEqualTo(mom);
		assertThat(message.getCreatedAt()).isNotNull();
	}

	@Test
	void findByFamilyIdReturnsLatestFirstWithinLimit() {
		List<ChatMessage> saved = saveTexts(kims, "1", "2", "3");
		chatMessageRepository.saveAndFlush(ChatMessage.text(parks, mom, "다른 집"));

		List<ChatMessage> found = chatMessageRepository.findByFamilyIdOrderByIdDesc(kims.getId(), Limit.of(2));

		assertThat(found).extracting(ChatMessage::getId)
				.containsExactly(saved.get(2).getId(), saved.get(1).getId());
	}

	@Test
	void findByCursorReturnsOnlyOlderMessages() {
		List<ChatMessage> saved = saveTexts(kims, "1", "2", "3", "4");

		List<ChatMessage> found = chatMessageRepository.findByFamilyIdAndIdLessThanOrderByIdDesc(kims.getId(),
				saved.get(2).getId(), Limit.of(10));

		assertThat(found).extracting(ChatMessage::getContent).containsExactly("2", "1");
	}

	@Test
	void deleteByFamilyIdDeletesOnlyThatFamily() {
		saveTexts(kims, "1", "2");
		saveTexts(parks, "3");

		int deleted = chatMessageRepository.deleteByFamilyId(kims.getId());

		assertThat(deleted).isEqualTo(2);
		assertThat(chatMessageRepository.findAll()).extracting(ChatMessage::getContent).containsExactly("3");
	}

	private List<ChatMessage> saveTexts(Family family, String... contents) {
		return chatMessageRepository.saveAllAndFlush(
				List.of(contents).stream().map(content -> ChatMessage.text(family, mom, content)).toList());
	}

}
