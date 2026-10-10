package com.urijip.server.member.repository;

import java.util.Optional;

import com.urijip.server.member.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByToken(String token);

	long deleteByTokenAndUserId(String token, Long userId);

}
