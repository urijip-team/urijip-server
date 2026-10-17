package com.urijip.server.photo.repository;

import java.util.List;
import java.util.Optional;

import com.urijip.server.photo.entity.PhotoComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoCommentRepository extends JpaRepository<PhotoComment, Long> {

	// 오래된 댓글부터. 작성자 이름을 함께 내보내므로 회원을 같이 읽는다.
	@EntityGraph(attributePaths = "user")
	List<PhotoComment> findByPhotoIdOrderByIdAsc(Long photoId);

	@EntityGraph(attributePaths = {"photo", "photo.album"})
	Optional<PhotoComment> findWithPhotoById(Long commentId);

	// 사진을 지우기 전에 댓글을 쿼리 한 번으로 지운다. 영속성 컨텍스트를 거치지 않는다.
	@Modifying
	@Query("delete from PhotoComment c where c.photo.id = :photoId")
	int deleteByPhotoId(@Param("photoId") Long photoId);

	@Modifying
	@Query("""
			delete from PhotoComment c
			where c.photo.id in (select p.id from Photo p where p.album.family.id = :familyId)
			""")
	int deleteByFamilyId(@Param("familyId") Long familyId);

}
