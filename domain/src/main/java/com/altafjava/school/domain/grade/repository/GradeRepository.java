package com.altafjava.school.domain.grade.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.grade.model.Grade;

public interface GradeRepository extends JpaRepository<Grade, Long> {

	Page<Grade> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Grade> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT g FROM Grade g WHERE g.tenantId = :tenantId AND g.studentId = :studentId")
	List<Grade> findByStudentId(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId);

	// Batched alternative to findByStudentId for callers (ReportCardService's class-rank
	// calculation) that need every classmate's grades in one query instead of one per student.
	@Query("SELECT g FROM Grade g WHERE g.tenantId = :tenantId AND g.studentId IN :studentIds")
	List<Grade> findByStudentIdInAndTenantId(@Param("studentIds") List<Long> studentIds,
			@Param("tenantId") Long tenantId);

	Page<Grade> findByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	@Query(value = "SELECT g FROM Grade g JOIN Exam e ON e.id = g.examId WHERE g.tenantId = :tenantId "
			+ "AND e.tenantId = :tenantId AND g.studentId = :studentId AND e.resultsPublishedAt IS NOT NULL", countQuery = "SELECT COUNT(g) FROM Grade g JOIN Exam e ON e.id = g.examId WHERE g.tenantId = :tenantId "
					+ "AND e.tenantId = :tenantId AND g.studentId = :studentId AND e.resultsPublishedAt IS NOT NULL")
	Page<Grade> findPublishedByStudentId(@Param("studentId") Long studentId, @Param("tenantId") Long tenantId,
			Pageable pageable);

	boolean existsByStudentIdAndExamIdAndTenantId(Long studentId, Long examId, Long tenantId);

	Page<Grade> findByExamIdInAndTenantId(List<Long> examIds, Long tenantId, Pageable pageable);

	boolean existsByExamIdAndTenantId(Long examId, Long tenantId);

	List<Grade> findAllByExamIdAndTenantId(Long examId, Long tenantId);

	long countByTenantId(Long tenantId);

	// Grade-letter distribution for the academic dashboard — grouped at the DB rather than pulled
	// row-by-row and counted in memory, matching FeePaymentRepository.sumPaidAmountByTenantId's
	// precedent for tenant-wide aggregates.
	@Query("SELECT g.gradeLetter, COUNT(g) FROM Grade g WHERE g.tenantId = :tenantId GROUP BY g.gradeLetter")
	List<Object[]> countGroupedByGradeLetter(@Param("tenantId") Long tenantId);
}
