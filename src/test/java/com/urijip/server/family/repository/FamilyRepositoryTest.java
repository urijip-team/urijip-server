package com.urijip.server.family.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyStatus;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class FamilyRepositoryTest {

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	private User owner;

	@BeforeEach
	void setUp() {
		owner = saveUser("mom@example.com", "김엄마");
	}

	@Test
	void savedFamilyKeepsNameCodeAndOwner() {
		Family saved = familyRepository.saveAndFlush(family("A3K9P2"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getName()).isEqualTo("김씨네");
		assertThat(saved.getInviteCode()).isEqualTo("A3K9P2");
		assertThat(saved.getOwner().getId()).isEqualTo(owner.getId());
	}

	@Test
	void savedFamilyGetsActiveStatusAndCreatedAt() {
		Family saved = familyRepository.saveAndFlush(family("A3K9P2"));

		assertThat(saved.getStatus()).isEqualTo(FamilyStatus.ACTIVE);
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void existsByInviteCodeFindsOnlySavedCode() {
		familyRepository.saveAndFlush(family("A3K9P2"));

		assertThat(familyRepository.existsByInviteCode("A3K9P2")).isTrue();
		assertThat(familyRepository.existsByInviteCode("ZZZZZZ")).isFalse();
	}

	private Family family(String inviteCode) {
		return Family.builder()
				.name("김씨네")
				.inviteCode(inviteCode)
				.owner(owner)
				.build();
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
