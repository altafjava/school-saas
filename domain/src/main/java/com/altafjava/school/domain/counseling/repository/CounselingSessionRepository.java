package com.altafjava.school.domain.counseling.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.counseling.model.CounselingSession;

public interface CounselingSessionRepository extends JpaRepository<CounselingSession, Long> {

	Page<CounselingSession> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all). Dates are inclusive.
	@Query("""
			SELECT c FROM CounselingSession c
			WHERE c.tenantId = :tenantId
			  AND (:studentId IS NULL OR c.studentId = :studentId)
			  AND (:followUpRequired IS NULL OR c.followUpRequired = :followUpRequired)
			  AND (:from IS NULL OR c.sessionDate >= :from)
			  AND (:to IS NULL OR c.sessionDate <= :to)
			""")
	Page<CounselingSession> search(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("followUpRequired") Boolean followUpRequired, @Param("from") LocalDate from,
			@Param("to") LocalDate to, Pageable pageable);

	Page<CounselingSession> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	/** Unpaged variant for GDPR/DPDP erasure and export, which must cover every matching row. */
	List<CounselingSession> findAllByStudentIdAndTenantId(Long studentId, Long tenantId);

	Optional<CounselingSession> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<CounselingSession> findByIdAndTenantId(Long id, Long tenantId);
}
