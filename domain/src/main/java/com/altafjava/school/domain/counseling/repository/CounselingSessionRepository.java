package com.altafjava.school.domain.counseling.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.counseling.model.CounselingSession;

public interface CounselingSessionRepository extends JpaRepository<CounselingSession, Long> {

	Page<CounselingSession> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<CounselingSession> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	/** Unpaged variant for GDPR/DPDP erasure and export, which must cover every matching row. */
	List<CounselingSession> findAllByStudentIdAndTenantId(Long studentId, Long tenantId);

	Optional<CounselingSession> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<CounselingSession> findByIdAndTenantId(Long id, Long tenantId);
}
