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
import com.altafjava.school.domain.attendance.model.PeriodAttendance;

public interface PeriodAttendanceRepository extends JpaRepository<PeriodAttendance, Long> {

	Page<PeriodAttendance> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<PeriodAttendance> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	// A caller's scoped view: the classrooms they teach plus the students who are theirs.
	@Query("""
			SELECT a FROM PeriodAttendance a
			WHERE a.tenantId = :tenantId
			  AND (a.classroomId IN :classroomIds OR a.studentId IN :studentIds)
			""")
	Page<PeriodAttendance> findVisible(@Param("tenantId") Long tenantId,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("studentIds") Collection<Long> studentIds,
			Pageable pageable);

	Page<PeriodAttendance> findByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	boolean existsByStudentIdAndTimetableEntryIdAndAttendanceDateAndTenantId(Long studentId, Long timetableEntryId,
			LocalDate attendanceDate, Long tenantId);
}
