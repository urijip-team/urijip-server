package com.urijip.server.place.repository;

import java.util.List;

import com.urijip.server.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	List<Place> findByFamilyIdOrderByIdAsc(Long familyId);

	long countByFamilyId(Long familyId);

	@Modifying
	@Query("delete from Place p where p.family.id = :familyId")
	int deleteByFamilyId(@Param("familyId") Long familyId);

}
