package com.urijip.server.family.repository;

import com.urijip.server.family.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

	boolean existsByUserId(Long userId);

}
