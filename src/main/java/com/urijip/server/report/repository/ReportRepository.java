package com.urijip.server.report.repository;

import java.util.List;

import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

	boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(Long reporterId, ReportTargetType targetType,
			Long targetId, ReportStatus status);

	List<Report> findByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId,
			ReportStatus status);

	// status가 null이면 모든 상태를 돌려준다.
	@EntityGraph(attributePaths = {"reporter", "handledBy"})
	@Query("select r from Report r where (:status is null or r.status = :status)")
	Page<Report> search(@Param("status") ReportStatus status, Pageable pageable);

}
