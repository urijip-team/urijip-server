package com.urijip.server.family.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.dto.request.FamilyCreateRequest;
import com.urijip.server.family.dto.response.FamilyResponse;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyRole;
import com.urijip.server.family.entity.FamilyStatus;
import com.urijip.server.family.repository.FamilyMemberRepository;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class FamilyServiceTest {

	@Autowired
	private FamilyService familyService;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private FamilyMemberRepository familyMemberRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
	}

	@Test
	void createReturnsFamilyWithInviteCode() {
		FamilyResponse response = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThat(response.id()).isNotNull();
		assertThat(response.name()).isEqualTo("김씨네");
		assertThat(response.inviteCode()).matches("[A-HJ-NP-Z2-9]{6}");
	}

	@Test
	void createSavesActiveFamilyOwnedByCreator() {
		FamilyResponse response = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		Family saved = familyRepository.findById(response.id()).orElseThrow();
		assertThat(saved.getOwner().getId()).isEqualTo(mom.getId());
		assertThat(saved.getStatus()).isEqualTo(FamilyStatus.ACTIVE);
		assertThat(saved.getInviteCode()).isEqualTo(response.inviteCode());
	}

	@Test
	void createRegistersCreatorAsOwnerMember() {
		FamilyResponse response = familyService.create(mom.getId(), new FamilyCreateRequest("김씨네"));

		assertThat(familyMemberRepository.findAll()).singleElement().satisfies(member -> {
			assertThat(member.getFamily().getId()).isEqualTo(response.id());
			assertThat(member.getUser().getId()).isEqualTo(mom.getId());
			assertThat(member.getRole()).isEqualTo(FamilyRole.OWNER);
		});
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
