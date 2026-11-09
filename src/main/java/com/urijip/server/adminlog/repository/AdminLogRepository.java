package com.urijip.server.adminlog.repository;

import com.urijip.server.adminlog.entity.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

	// adminId가 null이면 모든 관리자의 기록이다. 최신순이다.
	@EntityGraph(attributePaths = "admin")
	@Query(value = """
			select l from AdminLog l
			where :adminId is null or l.admin.id = :adminId
			order by l.id desc
			""", countQuery = """
			select count(l) from AdminLog l
			where :adminId is null or l.admin.id = :adminId
			""")
	Page<AdminLog> search(@Param("adminId") Long adminId, Pageable pageable);

}
