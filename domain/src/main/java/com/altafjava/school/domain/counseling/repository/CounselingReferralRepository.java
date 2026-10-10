package com.altafjava.school.domain.counseling.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.counseling.model.CounselingReferral;
import com.altafjava.school.domain.counseling.model.CounselingReferralStatus;

public interface CounselingReferralRepository extends JpaRepository<CounselingReferral, Long> {

	Page<CounselingReferral> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all). `from` is inclusive and `before` exclusive.
	@Query("""
			SELECT c FROM CounselingReferral c
			WHERE c.tenantId = :tenantId
			  AND (:studentId IS NULL OR c.studentId = :studentId)
			  AND (:status IS NULL OR c.status = :status)
			  AND (:from IS NULL OR c.referredAt >= :from)
			  AND (:before IS NULL OR c.referredAt < :before)
			""")
	Page<CounselingReferral> search(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("status") CounselingReferralStatus status, @Param("from") LocalDateTime from,
			@Param("before") LocalDateTime before, Pageable pageable);

	Page<CounselingReferral> findAllByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	Optional<CounselingReferral> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
