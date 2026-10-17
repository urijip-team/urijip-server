package com.urijip.server.chat.repository;

import java.util.List;

import com.urijip.server.chat.entity.ChatMessage;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	@EntityGraph(attributePaths = "sender")
	List<ChatMessage> findByFamilyIdOrderByIdDesc(Long familyId, Limit limit);

	// 커서(마지막으로 받은 메시지 id)보다 오래된 메시지
	@EntityGraph(attributePaths = "sender")
	List<ChatMessage> findByFamilyIdAndIdLessThanOrderByIdDesc(Long familyId, Long cursor, Limit limit);

	// 메시지가 많을 수 있어 하나씩 읽지 않고 쿼리 한 번으로 지운다. 영속성 컨텍스트를 거치지 않으므로
	// 같은 트랜잭션에서 읽어 둔 메시지 엔티티는 그대로 남는다.
	@Modifying
	@Query("delete from ChatMessage m where m.family.id = :familyId")
	int deleteByFamilyId(@Param("familyId") Long familyId);

}
