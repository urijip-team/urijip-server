package com.urijip.server.family.repository;

import java.util.List;
import java.util.Optional;

import com.urijip.server.family.entity.FamilyMember;
import com.urijip.server.family.entity.FamilyRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

	boolean existsByUserId(Long userId);

	boolean existsByFamilyIdAndUserId(Long familyId, Long userId);

	boolean existsByFamilyIdAndUserIdAndRole(Long familyId, Long userId, FamilyRole role);

	@EntityGraph(attributePaths = "family")
	Optional<FamilyMember> findFirstByUserId(Long userId);

	@EntityGraph(attributePaths = "user")
	List<FamilyMember> findByFamilyIdOrderByJoinedAtAscIdAsc(Long familyId);

	@EntityGraph(attributePaths = {"family", "user"})
	Optional<FamilyMember> findByFamilyIdAndUserId(Long familyId, Long userId);

}
