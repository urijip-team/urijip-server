package com.urijip.server.allowance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.allowance.entity.AllowanceRequest;
import com.urijip.server.allowance.entity.AllowanceStatus;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.repository.FamilyRepository;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class AllowanceRequestRepositoryTest {

	@Autowired
	private AllowanceRequestRepository allowanceRequestRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

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

	@Test
	void findSentReturnsOnlyMyRequestsInRangeNewestFirst() {
		User dad = saveUser("dad@example.com", "김아빠");
		AllowanceRequest early = saveAt(kid, mom, LocalDateTime.of(2026, 10, 1, 0, 0));
		AllowanceRequest late = saveAt(kid, dad, LocalDateTime.of(2026, 10, 31, 23, 59, 59));
		saveAt(kid, mom, LocalDateTime.of(2026, 9, 30, 23, 59, 59));
		saveAt(kid, mom, LocalDateTime.of(2026, 11, 1, 0, 0));
		saveAt(dad, mom, LocalDateTime.of(2026, 10, 15, 12, 0));

		entityManager.clear();

		assertThat(allowanceRequestRepository.findSent(kid.getId(), LocalDateTime.of(2026, 10, 1, 0, 0),
				LocalDateTime.of(2026, 11, 1, 0, 0)))
				.extracting(AllowanceRequest::getId).containsExactly(late.getId(), early.getId());
	}

	// created_at은 감사 기능이 넣고 updatable = false라서 SQL로 바꾼다. 읽기 전에 영속성 컨텍스트를 비운다.
	private AllowanceRequest saveAt(User requester, User receiver, LocalDateTime createdAt) {
		AllowanceRequest request = allowanceRequestRepository.saveAndFlush(
				new AllowanceRequest(kims, requester, receiver, 10000, null));
		jdbcTemplate.update("update allowance_requests set created_at = ? where id = ?", createdAt, request.getId());
		return request;
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.builder()
				.email(email)
				.password("encoded-password")
				.name(name)
				.build());
	}

}
