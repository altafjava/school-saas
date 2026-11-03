package com.altafjava.school.domain.exam.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
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
import com.altafjava.school.domain.exam.model.MarksStage;

public interface ExamRepository extends JpaRepository<Exam, Long> {

	/**
	 * Every filter is optional (null matches all). An exam whose schedule is not yet published is listed only
	 * for staff: everyone with {@code everySchedule}, or the classrooms in {@code staffClassroomIds}.
	 */
	@Query("""
			SELECT e FROM Exam e
			WHERE e.tenantId = :tenantId
			  AND (:everyClassroom = true OR e.classroomId IN :classroomIds)
			  AND (:everySchedule = true OR e.schedulePublishedAt IS NOT NULL OR e.resultsPublishedAt IS NOT NULL
			       OR e.classroomId IN :staffClassroomIds)
			  AND (:#{#classroomId == null} = TRUE OR e.classroomId = :classroomId)
			  AND (:#{#termId == null} = TRUE OR e.termId = :termId)
			  AND (:#{#subjectId == null} = TRUE OR e.subjectId = :subjectId)
			  AND (:#{#status == null} = TRUE OR e.status = :status)
			""")
	Page<Exam> search(@Param("tenantId") Long tenantId, @Param("everyClassroom") boolean everyClassroom,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("classroomId") Long classroomId,
			@Param("termId") Long termId, @Param("subjectId") Long subjectId, @Param("status") ExamStatus status,
			@Param("everySchedule") boolean everySchedule,
			@Param("staffClassroomIds") Collection<Long> staffClassroomIds,
			Pageable pageable);

	Optional<Exam> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	// Only what families can already see: a cancelled or unpublished exam is never announced.
	@Query("SELECT e FROM Exam e WHERE e.tenantId = :tenantId AND e.status = :status "
			+ "AND e.schedulePublishedAt IS NOT NULL AND e.scheduledAt BETWEEN :from AND :to")
	List<Exam> findPublishedBetween(@Param("tenantId") Long tenantId, @Param("status") ExamStatus status,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	Optional<Exam> findByIdAndTenantId(Long id, Long tenantId);

	List<Exam> findAllByIdInAndTenantId(List<Long> ids, Long tenantId);

	List<Exam> findAllByClassroomIdInAndTenantId(Collection<Long> classroomIds, Long tenantId);

	long countByTenantIdAndScheduledAtBetween(Long tenantId, LocalDateTime from, LocalDateTime to);

	// Weight already claimed in one subject's term result by the classroom's other live exams.
	@Query("SELECT COALESCE(SUM(e.weightage), 0) FROM Exam e WHERE e.tenantId = :tenantId "
			+ "AND e.classroomId = :classroomId AND e.subjectId = :subjectId AND e.termId = :termId "
			+ "AND e.status <> :excludedStatus AND e.id <> :excludedExamId")
	BigDecimal sumWeightageOfOtherExams(@Param("tenantId") Long tenantId, @Param("classroomId") Long classroomId,
			@Param("subjectId") Long subjectId, @Param("termId") Long termId,
			@Param("excludedStatus") ExamStatus excludedStatus, @Param("excludedExamId") Long excludedExamId);

	@Query("SELECT e FROM Exam e WHERE e.tenantId = :tenantId AND e.classroomId IN :classroomIds "
			+ "AND e.status = :status AND e.schedulePublishedAt IS NOT NULL AND e.scheduledAt BETWEEN :from AND :to "
			+ "ORDER BY e.scheduledAt, e.id")
	List<Exam> findPublishedInClassroomsBetween(@Param("tenantId") Long tenantId,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("status") ExamStatus status,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

	// Exams that have been sat and whose marks are still being entered, most recent first.
	@Query("SELECT e FROM Exam e WHERE e.tenantId = :tenantId AND e.classroomId IN :classroomIds "
			+ "AND e.status <> :excludedStatus AND e.scheduledAt <= :until AND e.marksStage = :stage "
			+ "ORDER BY e.scheduledAt DESC, e.id DESC")
	List<Exam> findWithMarksInStage(@Param("tenantId") Long tenantId,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("excludedStatus") ExamStatus excludedStatus,
			@Param("until") LocalDateTime until, @Param("stage") MarksStage stage, Pageable pageable);

	/**
	 * Live exams that start inside the window and share the classroom or, when given, the venue; the caller
	 * widens the window by the longest possible exam so one that started earlier and runs on is found too.
	 */
	@Query("""
			SELECT e FROM Exam e
			WHERE e.tenantId = :tenantId AND e.status <> :excludedStatus
			  AND e.scheduledAt > :from AND e.scheduledAt < :to
			  AND (e.classroomId = :classroomId OR (:#{#venueId == null} = FALSE AND e.venueId = :venueId))
			""")
	List<Exam> findClashCandidates(@Param("tenantId") Long tenantId, @Param("excludedStatus") ExamStatus excludedStatus,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("classroomId") Long classroomId,
			@Param("venueId") Long venueId);

	/** A classroom's exam timetable, in date order; the term narrows it when given. */
	@Query("""
			SELECT e FROM Exam e
			WHERE e.tenantId = :tenantId AND e.classroomId = :classroomId AND e.status <> :excludedStatus
			  AND (:#{#termId == null} = TRUE OR e.termId = :termId)
			  AND (:everySchedule = true OR e.schedulePublishedAt IS NOT NULL OR e.resultsPublishedAt IS NOT NULL)
			ORDER BY e.scheduledAt, e.id
			""")
	List<Exam> findTimetable(@Param("tenantId") Long tenantId, @Param("classroomId") Long classroomId,
			@Param("termId") Long termId, @Param("excludedStatus") ExamStatus excludedStatus,
			@Param("everySchedule") boolean everySchedule, Pageable pageable);
}
