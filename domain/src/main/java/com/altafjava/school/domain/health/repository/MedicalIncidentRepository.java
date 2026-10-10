package com.altafjava.school.domain.health.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.health.model.MedicalIncident;

public interface MedicalIncidentRepository extends JpaRepository<MedicalIncident, Long> {

	Page<MedicalIncident> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all). `from` is inclusive and `before` exclusive.
	@Query("""
			SELECT m FROM MedicalIncident m
			WHERE m.tenantId = :tenantId
			  AND (:studentId IS NULL OR m.studentId = :studentId)
			  AND (:from IS NULL OR m.occurredAt >= :from)
			  AND (:before IS NULL OR m.occurredAt < :before)
			""")
	Page<MedicalIncident> search(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("from") LocalDateTime from, @Param("before") LocalDateTime before, Pageable pageable);

	Page<MedicalIncident> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	/** Unpaged variant for GDPR/DPDP erasure and export, which must cover every matching row. */
	List<MedicalIncident> findAllByStudentIdAndTenantId(Long studentId, Long tenantId);

	Optional<MedicalIncident> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
