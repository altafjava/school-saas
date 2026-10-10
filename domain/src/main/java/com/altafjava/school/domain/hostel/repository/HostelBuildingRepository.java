package com.altafjava.school.domain.hostel.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.hostel.model.HostelBuilding;

public interface HostelBuildingRepository extends JpaRepository<HostelBuilding, Long> {

	Page<HostelBuilding> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT h FROM HostelBuilding h
			WHERE h.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(h.name) LIKE :pattern ESCAPE '!')
			""")
	Page<HostelBuilding> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Optional<HostelBuilding> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
