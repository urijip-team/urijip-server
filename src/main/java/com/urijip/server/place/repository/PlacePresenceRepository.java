package com.urijip.server.place.repository;

import java.util.List;

import com.urijip.server.place.entity.PlacePresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlacePresenceRepository extends JpaRepository<PlacePresence, Long> {

	@Query("select p.place.id from PlacePresence p where p.user.id = :userId")
	List<Long> findPlaceIdsByUserId(@Param("userId") Long userId);

	@Modifying
	@Query("delete from PlacePresence p where p.place.id = :placeId and p.user.id = :userId")
	int deleteByPlaceIdAndUserId(@Param("placeId") Long placeId, @Param("userId") Long userId);

	@Modifying
	@Query("delete from PlacePresence p where p.place.id = :placeId")
	int deleteByPlaceId(@Param("placeId") Long placeId);

	@Modifying
	@Query("delete from PlacePresence p where p.place.id in (select pl.id from Place pl where pl.family.id = :familyId)")
	int deleteByFamilyId(@Param("familyId") Long familyId);

}
