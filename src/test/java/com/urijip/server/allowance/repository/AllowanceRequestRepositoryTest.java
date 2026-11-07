package com.urijip.server.allowance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.allowance.entity.AllowanceRequest;
import com.urijip.server.allowance.entity.AllowanceStatus;
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

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class AllowanceRequestRepositoryTest {

	@Autowired
	private AllowanceRequestRepository allowanceRequestRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private User kid;

	private Family kims;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		kid = saveUser("kid@example.com", "김첫째");
		kims = familyRepository.saveAndFlush(Family.builder().name("김씨네").inviteCode("A3K9P2").owner(mom).build());
	}

	@Test
	void savesRequestedAllowance() {
		AllowanceRequest saved = allowanceRequestRepository.saveAndFlush(
				new AllowanceRequest(kims, kid, mom, 30000, "문제집 사야 해요"));

		AllowanceRequest found = allowanceRequestRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getFamily().getId()).isEqualTo(kims.getId());
		assertThat(found.getRequester().getId()).isEqualTo(kid.getId());
		assertThat(found.getReceiver().getId()).isEqualTo(mom.getId());
		assertThat(found.getAmount()).isEqualTo(30000);
		assertThat(found.getReason()).isEqualTo("문제집 사야 해요");
		assertThat(found.getStatus()).isEqualTo(AllowanceStatus.REQUESTED);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getCompletedAt()).isNull();
	}

	@Test
	void reasonIsOptional() {
		AllowanceRequest saved = allowanceRequestRepository.saveAndFlush(new AllowanceRequest(kims, kid, mom, 5000, null));

		assertThat(allowanceRequestRepository.findById(saved.getId()).orElseThrow().getReason()).isNull();
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
