package com.urijip.server.allowance.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.urijip.server.allowance.entity.AllowanceRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllowanceRequestRepository extends JpaRepository<AllowanceRequest, Long> {

	// 내가 보낸 요청 중 [from, to)에 만든 것. 최신순이다.
	@EntityGraph(attributePaths = {"family", "requester", "receiver"})
	@Query("""
			select a from AllowanceRequest a
			where a.requester.id = :userId and a.createdAt >= :from and a.createdAt < :to
			order by a.id desc
			""")
	List<AllowanceRequest> findSent(@Param("userId") Long userId, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	// 내가 받은 요청 중 [from, to)에 만든 것. 최신순이다.
	@EntityGraph(attributePaths = {"family", "requester", "receiver"})
	@Query("""
			select a from AllowanceRequest a
			where a.receiver.id = :userId and a.createdAt >= :from and a.createdAt < :to
			order by a.id desc
			""")
	List<AllowanceRequest> findReceived(@Param("userId") Long userId, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	@Modifying
	@Query("delete from AllowanceRequest a where a.family.id = :familyId")
	int deleteByFamilyId(@Param("familyId") Long familyId);

}
