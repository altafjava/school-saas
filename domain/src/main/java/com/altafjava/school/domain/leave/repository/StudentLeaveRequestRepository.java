package com.altafjava.school.domain.leave.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.StudentLeaveRequest;

public interface StudentLeaveRequestRepository extends JpaRepository<StudentLeaveRequest, Long> {

	Optional<StudentLeaveRequest> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	// Every filter is optional; a request matches when its leave overlaps the window.
	@Query("""
			SELECT r FROM StudentLeaveRequest r
			WHERE r.tenantId = :tenantId
			  AND (:everyClassroom = true OR r.classroomId IN :classroomIds OR r.studentId IN :studentIds)
			  AND (:#{#studentId == null} = TRUE OR r.studentId = :studentId)
			  AND (:#{#classroomId == null} = TRUE OR r.classroomId = :classroomId)
			  AND (:#{#status == null} = TRUE OR r.status = :status)
			  AND (:#{#from == null} = TRUE OR r.endDate >= :from)
			  AND (:#{#to == null} = TRUE OR r.startDate <= :to)
			""")
	Page<StudentLeaveRequest> search(@Param("tenantId") Long tenantId, @Param("everyClassroom") boolean everyClassroom,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("studentIds") Collection<Long> studentIds,
			@Param("studentId") Long studentId, @Param("classroomId") Long classroomId,
			@Param("status") LeaveRequestStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to,
			Pageable pageable);

	@Query("""
			SELECT COUNT(r) > 0 FROM StudentLeaveRequest r
			WHERE r.tenantId = :tenantId AND r.studentId = :studentId
			  AND r.status IN :statuses AND r.startDate <= :endDate AND r.endDate >= :startDate
			""")
	boolean existsOverlapping(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("statuses") Collection<LeaveRequestStatus> statuses, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	// What a class teacher can decide: never a request they raised themselves.
	@Query("""
			SELECT r FROM StudentLeaveRequest r
			WHERE r.tenantId = :tenantId AND r.status = :status AND r.classroomId IN :classroomIds
			  AND r.requestedByUserId <> :excludedRequesterId
			ORDER BY r.startDate, r.id
			""")
	Page<StudentLeaveRequest> findForReview(@Param("tenantId") Long tenantId,
			@Param("status") LeaveRequestStatus status, @Param("classroomIds") Collection<Long> classroomIds,
			@Param("excludedRequesterId") Long excludedRequesterId, Pageable pageable);

	@Query("SELECT r FROM StudentLeaveRequest r WHERE r.tenantId = :tenantId AND r.status = :status "
			+ "AND r.studentId IN :studentIds ORDER BY r.startDate, r.id")
	List<StudentLeaveRequest> findByStudentsAndStatus(@Param("tenantId") Long tenantId,
			@Param("status") LeaveRequestStatus status, @Param("studentIds") Collection<Long> studentIds,
			Pageable pageable);
}
