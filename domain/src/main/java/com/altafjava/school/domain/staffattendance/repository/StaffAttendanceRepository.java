package com.altafjava.school.domain.staffattendance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.staffattendance.model.StaffAttendance;
import com.altafjava.school.domain.staffattendance.model.StaffAttendanceStatus;

public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {

	Optional<StaffAttendance> findByEmployeeIdAndAttendanceDateAndTenantId(Long employeeId, LocalDate attendanceDate,
			Long tenantId);

	List<StaffAttendance> findAllByTenantIdAndAttendanceDateAndEmployeeIdIn(Long tenantId, LocalDate attendanceDate,
			Collection<Long> employeeIds);

	// Every filter is optional (null matches all); the dates are inclusive.
	@Query("""
			SELECT a FROM StaffAttendance a
			WHERE a.tenantId = :tenantId
			  AND (:#{#employeeId == null} = TRUE OR a.employeeId = :employeeId)
			  AND (:#{#status == null} = TRUE OR a.status = :status)
			  AND (:#{#from == null} = TRUE OR a.attendanceDate >= :from)
			  AND (:#{#to == null} = TRUE OR a.attendanceDate <= :to)
			""")
	Page<StaffAttendance> search(@Param("tenantId") Long tenantId, @Param("employeeId") Long employeeId,
			@Param("status") StaffAttendanceStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to,
			Pageable pageable);

	// Days the employee was at work; approving leave over them would pay the same day twice.
	@Query("""
			SELECT COUNT(a) > 0 FROM StaffAttendance a
			WHERE a.tenantId = :tenantId AND a.employeeId = :employeeId AND a.status <> :absent
			  AND a.attendanceDate BETWEEN :from AND :to
			""")
	boolean existsWorkedBetween(@Param("tenantId") Long tenantId, @Param("employeeId") Long employeeId,
			@Param("absent") StaffAttendanceStatus absent, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
