package com.urijip.server.sos.repository;

import com.urijip.server.sos.entity.SosAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SosAlertRepository extends JpaRepository<SosAlert, Long> {

	// 진행 중인 SOS를 먼저 보여 주고, 그 안에서는 최신순이다.
	@EntityGraph(attributePaths = {"user", "resolvedBy"})
	@Query("""
			select a from SosAlert a
			where a.family.id = :familyId
			order by case when a.resolvedAt is null then 0 else 1 end, a.id desc
			""")
	Page<SosAlert> findHistory(@Param("familyId") Long familyId, Pageable pageable);

}
