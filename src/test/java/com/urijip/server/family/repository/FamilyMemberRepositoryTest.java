package com.urijip.server.family.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urijip.server.TestcontainersConfiguration;
import com.urijip.server.family.entity.Family;
import com.urijip.server.family.entity.FamilyMember;
import com.urijip.server.family.entity.FamilyRole;
import com.urijip.server.global.config.JpaAuditingConfig;
import com.urijip.server.member.entity.User;
import com.urijip.server.member.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class FamilyMemberRepositoryTest {

	@Autowired
	private FamilyMemberRepository familyMemberRepository;

	@Autowired
	private FamilyRepository familyRepository;

	@Autowired
	private UserRepository userRepository;

	private User mom;

	private Family family;

	@BeforeEach
	void setUp() {
		mom = saveUser("mom@example.com", "김엄마");
		family = familyRepository.saveAndFlush(Family.builder()
				.name("김씨네")
				.inviteCode("A3K9P2")
				.owner(mom)
				.build());
	}

	@Test
	void savedMemberKeepsFamilyUserAndRole() {
		FamilyMember saved = familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getFamily().getId()).isEqualTo(family.getId());
		assertThat(saved.getUser().getId()).isEqualTo(mom.getId());
		assertThat(saved.getRole()).isEqualTo(FamilyRole.OWNER);
	}

	@Test
	void savedMemberGetsJoinedAtAndNoNickname() {
		FamilyMember saved = familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));

		assertThat(saved.getJoinedAt()).isNotNull();
		assertThat(saved.getNickname()).isNull();
	}

	@Test
	void existsByUserIdFindsOnlyUsersInFamily() {
		User dad = saveUser("dad@example.com", "김아빠");
		familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));

		assertThat(familyMemberRepository.existsByUserId(mom.getId())).isTrue();
		assertThat(familyMemberRepository.existsByUserId(dad.getId())).isFalse();
	}

	@Test
	void oneFamilyCanHaveSeveralMembers() {
		User dad = saveUser("dad@example.com", "김아빠");
		familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));
		familyMemberRepository.saveAndFlush(member(dad, FamilyRole.MEMBER));

		assertThat(familyMemberRepository.count()).isEqualTo(2);
	}

	@Test
	void sameUserTwiceInSameFamilyIsRejectedByUniqueConstraint() {
		familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));

		assertThatThrownBy(() -> familyMemberRepository.saveAndFlush(member(mom, FamilyRole.MEMBER)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findFirstByUserIdReturnsMembershipWithFamily() {
		familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));

		assertThat(familyMemberRepository.findFirstByUserId(mom.getId()))
				.get()
				.extracting(found -> found.getFamily().getName())
				.isEqualTo("김씨네");
	}

	@Test
	void findFirstByUserIdReturnsEmptyForUserWithoutFamily() {
		assertThat(familyMemberRepository.findFirstByUserId(mom.getId())).isEmpty();
	}

	@Test
	void existsByFamilyIdAndUserIdIsTrueOnlyForMemberOfThatFamily() {
		User dad = saveUser("dad@example.com", "김아빠");
		familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));

		assertThat(familyMemberRepository.existsByFamilyIdAndUserId(family.getId(), mom.getId())).isTrue();
		assertThat(familyMemberRepository.existsByFamilyIdAndUserId(family.getId(), dad.getId())).isFalse();
	}

	@Test
	void existsByFamilyIdAndUserIdIsFalseForAnotherFamilyId() {
		familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));

		assertThat(familyMemberRepository.existsByFamilyIdAndUserId(family.getId() + 1, mom.getId())).isFalse();
	}

	@Test
	void findByFamilyIdReturnsMembersInJoinOrder() {
		User dad = saveUser("dad@example.com", "김아빠");
		familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));
		familyMemberRepository.saveAndFlush(member(dad, FamilyRole.MEMBER));

		assertThat(familyMemberRepository.findByFamilyIdOrderByJoinedAtAscIdAsc(family.getId()))
				.extracting(found -> found.getUser().getName())
				.containsExactly("김엄마", "김아빠");
	}

	@Test
	void findByFamilyIdExcludesMembersOfAnotherFamily() {
		User dad = saveUser("dad@example.com", "김아빠");
		Family another = familyRepository.saveAndFlush(Family.builder()
				.name("아빠네")
				.inviteCode("B2B2B2")
				.owner(dad)
				.build());
		familyMemberRepository.saveAndFlush(member(mom, FamilyRole.OWNER));
		familyMemberRepository.saveAndFlush(FamilyMember.builder()
				.family(another)
				.user(dad)
				.role(FamilyRole.OWNER)
				.build());

		assertThat(familyMemberRepository.findByFamilyIdOrderByJoinedAtAscIdAsc(family.getId()))
				.extracting(found -> found.getUser().getName())
				.containsExactly("김엄마");
	}

	private FamilyMember member(User user, FamilyRole role) {
		return FamilyMember.builder()
				.family(family)
				.user(user)
				.role(role)
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
