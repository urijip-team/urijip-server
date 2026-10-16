package com.urijip.server.family.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.urijip.server.family.entity.FamilyMember;
import com.urijip.server.family.entity.FamilyRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

	boolean existsByUserId(Long userId);

	boolean existsByFamilyIdAndUserId(Long familyId, Long userId);

	long countByFamilyId(Long familyId);

	// 여러 가족의 멤버 수를 쿼리 한 번으로 센다. 멤버가 없는 가족은 결과에 없다.
	@Query("""
			select m.family.id as familyId, count(m) as memberCount
			from FamilyMember m
			where m.family.id in :familyIds
			group by m.family.id
			""")
	List<FamilyMemberCount> countByFamilyIds(@Param("familyIds") Collection<Long> familyIds);

	long countByFamilyIdAndUserIdIn(Long familyId, Collection<Long> userIds);

	boolean existsByFamilyIdAndUserIdAndRole(Long familyId, Long userId, FamilyRole role);

	@EntityGraph(attributePaths = "family")
	Optional<FamilyMember> findFirstByUserId(Long userId);

	@EntityGraph(attributePaths = "user")
	List<FamilyMember> findByFamilyIdOrderByJoinedAtAscIdAsc(Long familyId);

	@EntityGraph(attributePaths = {"family", "user"})
	Optional<FamilyMember> findByFamilyIdAndUserId(Long familyId, Long userId);

}
