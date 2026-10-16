package com.urijip.server.schedule.repository;

import java.util.List;

import com.urijip.server.schedule.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {

	// 마감일이 빠른 순, 마감일이 없는 할 일은 뒤로. done이 null이면 완료 여부와 상관없이 모두.
	@Query("""
			select t from Todo t
			where t.family.id = :familyId and (:done is null or t.done = :done)
			order by case when t.dueDate is null then 1 else 0 end, t.dueDate, t.id
			""")
	List<Todo> findByFamily(@Param("familyId") Long familyId, @Param("done") Boolean done);

	long deleteByFamilyId(Long familyId);

}
