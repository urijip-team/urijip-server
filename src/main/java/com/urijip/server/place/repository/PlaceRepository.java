package com.urijip.server.place.repository;

import java.util.List;

import com.urijip.server.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	List<Place> findByFamilyIdOrderByIdAsc(Long familyId);

	long countByFamilyId(Long familyId);

}
