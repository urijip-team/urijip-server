package com.urijip.server.photo.repository;

import java.util.List;

import com.urijip.server.photo.entity.Album;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {

	@EntityGraph(attributePaths = "coverPhoto")
	List<Album> findByFamilyIdOrderByIdDesc(Long familyId);

	List<Album> findByFamilyId(Long familyId);

}
