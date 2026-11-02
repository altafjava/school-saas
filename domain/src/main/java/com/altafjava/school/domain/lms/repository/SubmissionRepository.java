package com.altafjava.school.domain.lms.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.lms.model.GradingQueueEntry;
import com.altafjava.school.domain.lms.model.Submission;
import com.altafjava.school.domain.lms.model.SubmissionStatus;
import com.altafjava.school.domain.lms.model.SubmittedWork;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

	Optional<Submission> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT s FROM Submission s WHERE s.assignmentId = :assignmentId AND s.tenantId = :tenantId")
	Page<Submission> findByAssignmentIdAndTenantId(@Param("assignmentId") Long assignmentId,
			@Param("tenantId") Long tenantId, Pageable pageable);

	@Query("SELECT s FROM Submission s WHERE s.assignmentId = :assignmentId AND s.studentId = :studentId "
			+ "AND s.tenantId = :tenantId")
	Optional<Submission> findByAssignmentIdAndStudentIdAndTenantId(@Param("assignmentId") Long assignmentId,
			@Param("studentId") Long studentId, @Param("tenantId") Long tenantId);

	boolean existsByAssignmentIdAndStudentIdAndTenantId(Long assignmentId, Long studentId, Long tenantId);

	@Query("""
			SELECT new com.altafjava.school.domain.lms.model.SubmittedWork(s.studentId, s.assignmentId)
			FROM Submission s
			WHERE s.tenantId = :tenantId AND s.studentId IN :studentIds AND s.assignmentId IN :assignmentIds
			""")
	List<SubmittedWork> findSubmittedWork(@Param("tenantId") Long tenantId,
			@Param("studentIds") Collection<Long> studentIds, @Param("assignmentIds") Collection<Long> assignmentIds);

	/** A teacher's assignments with work to grade, oldest due date first. */
	@Query(value = """
			SELECT new com.altafjava.school.domain.lms.model.GradingQueueEntry(a.publicId, a.title, a.classroomId,
			       a.subjectId, a.dueDate, COUNT(s))
			FROM Submission s JOIN Assignment a ON a.id = s.assignmentId
			WHERE s.tenantId = :tenantId AND a.tenantId = :tenantId AND a.teacherId = :teacherId
			  AND a.classroomId IN :classroomIds AND s.status IN :statuses
			GROUP BY a.id, a.publicId, a.title, a.classroomId, a.subjectId, a.dueDate
			ORDER BY a.dueDate, a.id
			""", countQuery = """
			SELECT COUNT(DISTINCT a.id)
			FROM Submission s JOIN Assignment a ON a.id = s.assignmentId
			WHERE s.tenantId = :tenantId AND a.tenantId = :tenantId AND a.teacherId = :teacherId
			  AND a.classroomId IN :classroomIds AND s.status IN :statuses
			""")
	Page<GradingQueueEntry> findGradingQueue(@Param("tenantId") Long tenantId, @Param("teacherId") Long teacherId,
			@Param("classroomIds") Collection<Long> classroomIds,
			@Param("statuses") Collection<SubmissionStatus> statuses, Pageable pageable);
}
