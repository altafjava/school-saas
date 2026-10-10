package com.altafjava.school.domain.alumni.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.alumni.model.AlumniProfile;

public interface AlumniProfileRepository extends JpaRepository<AlumniProfile, Long> {

	Page<AlumniProfile> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all).
	@Query("""
			SELECT a FROM AlumniProfile a
			WHERE a.tenantId = :tenantId
			  AND (:graduationYear IS NULL OR a.graduationYear = :graduationYear)
			  AND (:active IS NULL OR a.active = :active)
			  AND (:pattern IS NULL OR LOWER(a.currentOccupation) LIKE :pattern ESCAPE '!' OR LOWER(a.contactEmail) LIKE :pattern ESCAPE '!')
			""")
	Page<AlumniProfile> search(@Param("tenantId") Long tenantId, @Param("graduationYear") Integer graduationYear,
			@Param("active") Boolean active, @Param("pattern") String pattern, Pageable pageable);

	Optional<AlumniProfile> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByStudentIdAndTenantId(Long studentId, Long tenantId);
}
