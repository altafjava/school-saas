package com.altafjava.school.domain.attendance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;

public interface PeriodAttendanceRepository extends JpaRepository<PeriodAttendance, Long> {

	Page<PeriodAttendance> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<PeriodAttendance> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	/**
	 * The period-attendance list. Scope first: every classroom, or only the classrooms the caller teaches plus
	 * the students who are theirs. Every other filter is optional (null matches all) and narrows within that scope.
	 */
	@Query("""
			SELECT a FROM PeriodAttendance a
			WHERE a.tenantId = :tenantId
			  AND (:allClassrooms = true OR a.classroomId IN :classroomIds OR a.studentId IN :studentIds)
			  AND (:classroomId IS NULL OR a.classroomId = :classroomId)
			  AND (:studentId IS NULL OR a.studentId = :studentId)
			  AND (:from IS NULL OR a.attendanceDate >= :from)
			  AND (:to IS NULL OR a.attendanceDate <= :to)
			  AND (:status IS NULL OR a.status = :status)
			""")
	Page<PeriodAttendance> search(@Param("tenantId") Long tenantId, @Param("allClassrooms") boolean allClassrooms,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("studentIds") Collection<Long> studentIds,
			@Param("classroomId") Long classroomId, @Param("studentId") Long studentId,
			@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("status") AttendanceStatus status,
			Pageable pageable);

	Page<PeriodAttendance> findByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	boolean existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(Long studentId, Long timetableEntryId,
			LocalDate attendanceDate, Long tenantId);
}
