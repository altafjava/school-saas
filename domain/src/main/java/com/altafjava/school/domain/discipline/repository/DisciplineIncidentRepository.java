package com.altafjava.school.domain.discipline.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.discipline.model.DisciplineIncident;
import com.altafjava.school.domain.discipline.model.IncidentSeverity;

public interface DisciplineIncidentRepository extends JpaRepository<DisciplineIncident, Long> {

	Page<DisciplineIncident> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all). Dates are inclusive.
	@Query("""
			SELECT d FROM DisciplineIncident d
			WHERE d.tenantId = :tenantId
			  AND (:studentId IS NULL OR d.studentId = :studentId)
			  AND (:severity IS NULL OR d.severity = :severity)
			  AND (:from IS NULL OR d.incidentDate >= :from)
			  AND (:to IS NULL OR d.incidentDate <= :to)
			""")
	Page<DisciplineIncident> search(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("severity") IncidentSeverity severity, @Param("from") LocalDate from, @Param("to") LocalDate to,
			Pageable pageable);

	Page<DisciplineIncident> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	/** Unpaged variant for GDPR/DPDP erasure and export, which must cover every matching row. */
	List<DisciplineIncident> findAllByStudentIdAndTenantId(Long studentId, Long tenantId);

	Optional<DisciplineIncident> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
