package com.altafjava.school.domain.hostel.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.hostel.model.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

	Page<Room> findAllByHostelBuildingIdAndTenantId(Long hostelBuildingId, Long tenantId, Pageable pageable);

	// Blank q matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT r FROM Room r
			WHERE r.tenantId = :tenantId AND r.hostelBuildingId = :hostelBuildingId
			  AND (:pattern IS NULL OR LOWER(r.roomNumber) LIKE :pattern ESCAPE '!')
			""")
	Page<Room> search(@Param("tenantId") Long tenantId, @Param("hostelBuildingId") Long hostelBuildingId,
			@Param("pattern") String pattern, Pageable pageable);

	Optional<Room> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Room> findByIdAndTenantId(Long id, Long tenantId);
}
