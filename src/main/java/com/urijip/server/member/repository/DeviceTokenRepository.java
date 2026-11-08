package com.urijip.server.member.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.urijip.server.member.entity.DeviceToken;
import com.urijip.server.member.entity.Role;
import com.urijip.server.member.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

	Optional<DeviceToken> findByFcmToken(String fcmToken);

	long deleteByFcmTokenAndUserId(String fcmToken, Long userId);

	long deleteByUserId(Long userId);

	@Query("select t.fcmToken from DeviceToken t where t.user.id in :userIds")
	List<String> findFcmTokensByUserIds(@Param("userIds") Collection<Long> userIds);

	@Query("select t.fcmToken from DeviceToken t where t.user.role = :role and t.user.status = :status")
	List<String> findFcmTokensByUserRoleAndStatus(@Param("role") Role role, @Param("status") UserStatus status);

	// excludedUserIds는 비어 있으면 안 된다. 빈 목록이면 위 쿼리를 쓴다.
	@Query("""
			select t.fcmToken from DeviceToken t
			where t.user.role = :role and t.user.status = :status and t.user.id not in :excludedUserIds
			""")
	List<String> findFcmTokensByUserRoleAndStatusExcluding(@Param("role") Role role,
			@Param("status") UserStatus status, @Param("excludedUserIds") Collection<Long> excludedUserIds);

	@Modifying
	@Query("delete from DeviceToken t where t.fcmToken in :fcmTokens")
	int deleteByFcmTokens(@Param("fcmTokens") Collection<String> fcmTokens);

}
