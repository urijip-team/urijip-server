package com.urijip.server.schedule.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.urijip.server.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	// [from, to) 구간과 하루라도 겹치는 일정. 여러 달에 걸친 일정도 포함된다.
	@EntityGraph(attributePaths = "participantIds")
	@Query("""
			select s from Schedule s
			where s.family.id = :familyId and s.startAt < :to and s.endAt >= :from
			order by s.startAt, s.id
			""")
	List<Schedule> findOverlapping(@Param("familyId") Long familyId, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	long deleteByFamilyId(Long familyId);

}
