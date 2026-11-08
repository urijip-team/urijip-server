package com.urijip.server.inquiry.repository;

import com.urijip.server.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

	Page<Inquiry> findByUserId(Long userId, Pageable pageable);

	long countByAnsweredAtIsNull();

	// answered가 null이면 모두 돌려준다. 미답변을 먼저 보여 주고, 그 안에서는 최신순이다.
	@EntityGraph(attributePaths = {"user", "answeredBy"})
	@Query("""
			select i from Inquiry i
			where :answered is null
				or (:answered = true and i.answeredAt is not null)
				or (:answered = false and i.answeredAt is null)
			order by case when i.answeredAt is null then 0 else 1 end, i.id desc
			""")
	Page<Inquiry> search(@Param("answered") Boolean answered, Pageable pageable);

}
