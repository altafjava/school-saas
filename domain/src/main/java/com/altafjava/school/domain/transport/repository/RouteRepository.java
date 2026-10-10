package com.altafjava.school.domain.transport.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.transport.model.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {

	Page<Route> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT r FROM Route r
			WHERE r.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(r.name) LIKE :pattern ESCAPE '!'
			       OR LOWER(r.code) LIKE :pattern ESCAPE '!')
			""")
	Page<Route> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Optional<Route> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Route> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);
}
