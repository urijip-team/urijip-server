package com.urijip.server.report.repository;

import java.util.Collection;
import java.util.List;

import com.urijip.server.report.entity.Report;
import com.urijip.server.report.entity.ReportStatus;
import com.urijip.server.report.entity.ReportTargetType;
import org.springframework.data.domain.Limit;
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

	long countByStatus(ReportStatus status);

	// 회원 자신(USER)이나 그 회원이 올린 사진(PHOTO)에 대한 신고, 최신순. photoIds는 비어 있어도 된다.
	@Query("""
			select r from Report r
			where (r.targetType = com.urijip.server.report.entity.ReportTargetType.USER and r.targetId = :userId)
			or (r.targetType = com.urijip.server.report.entity.ReportTargetType.PHOTO and r.targetId in :photoIds)
			order by r.id desc
			""")
	List<Report> findAgainstUser(@Param("userId") Long userId, @Param("photoIds") Collection<Long> photoIds,
			Limit limit);

	@Query("""
			select count(r) from Report r
			where (r.targetType = com.urijip.server.report.entity.ReportTargetType.USER and r.targetId = :userId)
			or (r.targetType = com.urijip.server.report.entity.ReportTargetType.PHOTO and r.targetId in :photoIds)
			""")
	long countAgainstUser(@Param("userId") Long userId, @Param("photoIds") Collection<Long> photoIds);

	// status가 null이면 모든 상태를 돌려준다.
	@EntityGraph(attributePaths = {"reporter", "handledBy"})
	@Query("select r from Report r where (:status is null or r.status = :status)")
	Page<Report> search(@Param("status") ReportStatus status, Pageable pageable);

}
