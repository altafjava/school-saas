package com.altafjava.school.domain.reportcard.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.reportcard.model.ReportCard;

public interface ReportCardRepository extends JpaRepository<ReportCard, Long> {

	Page<ReportCard> findByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	Optional<ReportCard> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByStudentIdAndTermIdAndTenantId(Long studentId, Long termId, Long tenantId);

	Optional<ReportCard> findByStudentIdAndTermIdAndTenantId(Long studentId, Long termId, Long tenantId);

	/**
	 * Student ids with a report card for {@code termId} created at or after {@code since} — used
	 * by {@code ReportCardGenerationJob} to short-circuit students a just-crashed, now-retried
	 * attempt of the same batch run already finished, rather than re-rendering and re-uploading
	 * their PDF. Bounded to "created recently", not "a report card exists at all", so a student
	 * manually regenerated mid-term (see {@code StudentController.generateReportCard}) still gets
	 * a fresh one from the real end-of-term batch run.
	 */
	List<Long> findStudentIdByTermIdAndTenantIdAndCreatedAtGreaterThanEqual(Long termId, Long tenantId,
			Instant since);
}
