package com.altafjava.school.domain.health.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.health.model.MedicalIncident;

public interface MedicalIncidentRepository extends JpaRepository<MedicalIncident, Long> {

	Page<MedicalIncident> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<MedicalIncident> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	/** Unpaged variant for GDPR/DPDP erasure and export, which must cover every matching row. */
	List<MedicalIncident> findAllByStudentIdAndTenantId(Long studentId, Long tenantId);

	Optional<MedicalIncident> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
