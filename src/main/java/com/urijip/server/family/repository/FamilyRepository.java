package com.urijip.server.family.repository;

import java.util.Optional;

import com.urijip.server.family.entity.Family;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyRepository extends JpaRepository<Family, Long> {

	boolean existsByInviteCode(String inviteCode);

	Optional<Family> findByInviteCode(String inviteCode);

}
