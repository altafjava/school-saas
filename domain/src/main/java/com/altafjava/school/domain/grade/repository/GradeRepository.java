package com.altafjava.school.domain.grade.repository;

import java.util.Collection;
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

	// A caller's scoped view: every grade of the exams they teach, plus published grades of their own students.
	/**
	 * The grade list. Scope first: every grade, or only those of the exams the caller teaches plus the published
	 * grades of the students who are theirs. Every other filter is optional (null matches all) and narrows within
	 * that scope; the classroom is the one the grade's exam was set for.
	 */
	@Query(value = """
			SELECT g FROM Grade g
			WHERE g.tenantId = :tenantId
			  AND (:allClassrooms = true
			       OR g.examId IN :examIds
			       OR (g.studentId IN :studentIds
			           AND EXISTS (SELECT 1 FROM Exam e WHERE e.id = g.examId AND e.tenantId = :tenantId
			                       AND e.resultsPublishedAt IS NOT NULL)))
			  AND (:examId IS NULL OR g.examId = :examId)
			  AND (:studentId IS NULL OR g.studentId = :studentId)
			  AND (:classroomId IS NULL
			       OR EXISTS (SELECT 1 FROM Exam c WHERE c.id = g.examId AND c.tenantId = :tenantId
			                  AND c.classroomId = :classroomId))
			""", countQuery = """
			SELECT COUNT(g) FROM Grade g
			WHERE g.tenantId = :tenantId
			  AND (:allClassrooms = true
			       OR g.examId IN :examIds
			       OR (g.studentId IN :studentIds
			           AND EXISTS (SELECT 1 FROM Exam e WHERE e.id = g.examId AND e.tenantId = :tenantId
			                       AND e.resultsPublishedAt IS NOT NULL)))
			  AND (:examId IS NULL OR g.examId = :examId)
			  AND (:studentId IS NULL OR g.studentId = :studentId)
			  AND (:classroomId IS NULL
			       OR EXISTS (SELECT 1 FROM Exam c WHERE c.id = g.examId AND c.tenantId = :tenantId
			                  AND c.classroomId = :classroomId))
			""")
	Page<Grade> search(@Param("tenantId") Long tenantId, @Param("allClassrooms") boolean allClassrooms,
			@Param("examIds") Collection<Long> examIds, @Param("studentIds") Collection<Long> studentIds,
			@Param("examId") Long examId, @Param("studentId") Long studentId,
			@Param("classroomId") Long classroomId, Pageable pageable);

	boolean existsByExamIdAndTenantId(Long examId, Long tenantId);

	List<Grade> findAllByExamIdAndTenantId(Long examId, Long tenantId);

	long countByTenantId(Long tenantId);

	// Grade-letter distribution for the academic dashboard — grouped at the DB rather than pulled
	// row-by-row and counted in memory, matching FeePaymentRepository.sumPaidAmountByTenantId's
	// precedent for tenant-wide aggregates.
	@Query("SELECT g.gradeLetter, COUNT(g) FROM Grade g WHERE g.tenantId = :tenantId GROUP BY g.gradeLetter")
	List<Object[]> countGroupedByGradeLetter(@Param("tenantId") Long tenantId);
}
