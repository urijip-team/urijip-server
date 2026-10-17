package com.urijip.server.photo.repository;

import java.util.List;
import java.util.Optional;

import com.urijip.server.photo.entity.PhotoComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoCommentRepository extends JpaRepository<PhotoComment, Long> {

	// 오래된 댓글부터. 작성자 이름을 함께 내보내므로 회원을 같이 읽는다.
	@EntityGraph(attributePaths = "user")
	List<PhotoComment> findByPhotoIdOrderByIdAsc(Long photoId);

	@EntityGraph(attributePaths = {"photo", "photo.album"})
	Optional<PhotoComment> findWithPhotoById(Long commentId);

}
