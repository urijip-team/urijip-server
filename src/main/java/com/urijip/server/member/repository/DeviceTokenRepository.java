package com.urijip.server.member.repository;

import java.util.Optional;

import com.urijip.server.member.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

	Optional<DeviceToken> findByFcmToken(String fcmToken);

	long deleteByFcmTokenAndUserId(String fcmToken, Long userId);

	long deleteByUserId(Long userId);

}
