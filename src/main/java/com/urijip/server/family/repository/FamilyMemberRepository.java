package com.urijip.server.family.repository;

import java.util.List;
import java.util.Optional;

import com.urijip.server.family.entity.FamilyMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

	boolean existsByUserId(Long userId);

	boolean existsByFamilyIdAndUserId(Long familyId, Long userId);

	@EntityGraph(attributePaths = "family")
	Optional<FamilyMember> findFirstByUserId(Long userId);

	@EntityGraph(attributePaths = "user")
	List<FamilyMember> findByFamilyIdOrderByJoinedAtAscIdAsc(Long familyId);

}
