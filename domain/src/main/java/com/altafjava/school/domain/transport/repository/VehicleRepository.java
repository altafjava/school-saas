package com.altafjava.school.domain.transport.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.transport.model.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

	Page<Vehicle> findAllByTenantId(Long tenantId, Pageable pageable);

	// Blank {@code q} matches all; the pattern comes from LikePattern.contains.
	@Query("""
			SELECT v FROM Vehicle v
			WHERE v.tenantId = :tenantId
			  AND (:pattern IS NULL OR LOWER(v.registrationNumber) LIKE :pattern ESCAPE '!'
			       OR LOWER(v.driverName) LIKE :pattern ESCAPE '!')
			""")
	Page<Vehicle> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Optional<Vehicle> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Vehicle> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByRegistrationNumberAndTenantId(String registrationNumber, Long tenantId);
}
