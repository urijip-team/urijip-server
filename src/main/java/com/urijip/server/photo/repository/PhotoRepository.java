package com.urijip.server.photo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.urijip.server.photo.entity.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

	@EntityGraph(attributePaths = "uploader")
	Page<Photo> findByAlbumId(Long albumId, Pageable pageable);

	@EntityGraph(attributePaths = {"album", "album.family"})
	Optional<Photo> findWithAlbumById(Long photoId);

	@EntityGraph(attributePaths = "uploader")
	List<Photo> findByIdIn(Collection<Long> photoIds);

	Optional<Photo> findFirstByAlbumIdAndIdNotOrderByIdDesc(Long albumId, Long excludedPhotoId);

	// 여러 앨범의 사진 수를 쿼리 한 번으로 센다. 사진이 없는 앨범은 결과에 없다.
	@Query("""
			select p.album.id as albumId, count(p) as photoCount
			from Photo p
			where p.album.id in :albumIds
			group by p.album.id
			""")
	List<AlbumPhotoCount> countByAlbumIds(@Param("albumIds") Collection<Long> albumIds);

	List<Photo> findByAlbumFamilyId(Long familyId);

}
