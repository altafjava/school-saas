package com.altafjava.school.domain.exam.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;

public interface ExamRepository extends JpaRepository<Exam, Long> {

	Page<Exam> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Exam> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT e FROM Exam e WHERE e.tenantId = :tenantId AND e.scheduledAt BETWEEN :from AND :to")
	List<Exam> findUpcoming(@Param("tenantId") Long tenantId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	Optional<Exam> findByIdAndTenantId(Long id, Long tenantId);

	List<Exam> findAllByIdInAndTenantId(List<Long> ids, Long tenantId);

	@Query("SELECT e.id FROM Exam e WHERE e.tenantId = :tenantId AND e.classroomId IN :classroomIds")
	List<Long> findIdsByClassroomIdInAndTenantId(@Param("classroomIds") List<Long> classroomIds,
			@Param("tenantId") Long tenantId);

	long countByTenantIdAndScheduledAtBetween(Long tenantId, LocalDateTime from, LocalDateTime to);

	// Weight already claimed in one subject's term result by the classroom's other live exams.
	@Query("SELECT COALESCE(SUM(e.weightage), 0) FROM Exam e WHERE e.tenantId = :tenantId "
			+ "AND e.classroomId = :classroomId AND e.subjectId = :subjectId AND e.termId = :termId "
			+ "AND e.status <> :excludedStatus AND e.id <> :excludedExamId")
	BigDecimal sumWeightageOfOtherExams(@Param("tenantId") Long tenantId, @Param("classroomId") Long classroomId,
			@Param("subjectId") Long subjectId, @Param("termId") Long termId,
			@Param("excludedStatus") ExamStatus excludedStatus, @Param("excludedExamId") Long excludedExamId);
}
