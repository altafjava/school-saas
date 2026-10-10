package com.altafjava.school.domain.timetable.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.timetable.model.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {

	Page<Venue> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT v FROM Venue v
			WHERE v.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(v.name) LIKE :pattern ESCAPE '!'
			       OR LOWER(v.code) LIKE :pattern ESCAPE '!')
			""")
	Page<Venue> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Optional<Venue> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Venue> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
