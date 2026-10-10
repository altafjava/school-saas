package com.altafjava.school.domain.attendance.repository;

import java.time.Instant;
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
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	Page<Attendance> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Attendance> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT a FROM Attendance a WHERE a.tenantId = :tenantId AND a.classroomId = :classroomId AND a.attendanceDate = :date")
	List<Attendance> findByClassroomAndDate(@Param("tenantId") Long tenantId,
			@Param("classroomId") Long classroomId,
			@Param("date") LocalDate date);

	Optional<Attendance> findByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(Long studentId, Long classroomId,
			LocalDate attendanceDate, Long tenantId);

	boolean existsByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(Long studentId, Long classroomId,
			LocalDate attendanceDate, Long tenantId);

	boolean existsByClassroomIdAndAttendanceDateAndTenantId(Long classroomId, LocalDate attendanceDate,
			Long tenantId);

	Page<Attendance> findByStudentIdAndTenantId(Long studentId, Long tenantId, Pageable pageable);

	// Roster inferred from attendance history — retained for ExamScheduleReminderJob's existing call
	// site; StudentClassroomLinkRepository is now the authoritative source of enrollment, use that.
	@Query("SELECT DISTINCT a.studentId FROM Attendance a WHERE a.tenantId = :tenantId AND a.classroomId = :classroomId")
	List<Long> findDistinctStudentIdsByClassroomId(@Param("tenantId") Long tenantId,
			@Param("classroomId") Long classroomId);

	long countByTenantIdAndAttendanceDateBetweenAndStatus(Long tenantId, LocalDate from, LocalDate to,
			AttendanceStatus status);

	long countByTenantIdAndAttendanceDateBetween(Long tenantId, LocalDate from, LocalDate to);

	// A caller's scoped view: the classrooms they teach plus the students who are theirs.
	@Query("""
			SELECT a FROM Attendance a
			WHERE a.tenantId = :tenantId
			  AND (a.classroomId IN :classroomIds OR a.studentId IN :studentIds)
			""")
	Page<Attendance> findVisible(@Param("tenantId") Long tenantId,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("studentIds") Collection<Long> studentIds,
			Pageable pageable);

	@Query("""
			SELECT a FROM Attendance a
			WHERE a.tenantId = :tenantId AND a.updatedAt > :updatedAt
			  AND (a.classroomId IN :classroomIds OR a.studentId IN :studentIds)
			""")
	List<Attendance> findVisibleUpdatedAfter(@Param("tenantId") Long tenantId, @Param("updatedAt") Instant updatedAt,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("studentIds") Collection<Long> studentIds);

	long countByStudentIdAndTenantIdAndAttendanceDateBetween(Long studentId, Long tenantId, LocalDate from,
			LocalDate to);

	long countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus(Long studentId, Long tenantId, LocalDate from,
			LocalDate to, AttendanceStatus status);

	/**
	 * Same as {@link #countByStudentIdAndTenantIdAndAttendanceDateBetween}, excluding any date in
	 * {@code excludedDates} (the tenant's holiday calendar) from both the range and the count —
	 * only called when that set is non-empty, see {@code AttendanceService#calculatePercentage}.
	 */
	@Query("SELECT COUNT(a) FROM Attendance a WHERE a.studentId = :studentId AND a.tenantId = :tenantId "
			+ "AND a.attendanceDate BETWEEN :from AND :to AND a.attendanceDate NOT IN :excludedDates")
	long countByStudentIdAndTenantIdAndAttendanceDateBetweenExcludingDates(@Param("studentId") Long studentId,
			@Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to,
			@Param("excludedDates") Collection<LocalDate> excludedDates);

	/** Same as {@link #countByStudentIdAndTenantIdAndAttendanceDateBetweenExcludingDates}, filtered by status. */
	@Query("SELECT COUNT(a) FROM Attendance a WHERE a.studentId = :studentId AND a.tenantId = :tenantId "
			+ "AND a.attendanceDate BETWEEN :from AND :to AND a.status = :status "
			+ "AND a.attendanceDate NOT IN :excludedDates")
	long countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatusExcludingDates(
			@Param("studentId") Long studentId, @Param("tenantId") Long tenantId, @Param("from") LocalDate from,
			@Param("to") LocalDate to, @Param("status") AttendanceStatus status,
			@Param("excludedDates") Collection<LocalDate> excludedDates);

	/**
	 * One row per {@code studentId} present in {@code studentIds} — batched alternative to calling
	 * {@link #countByStudentIdAndTenantIdAndAttendanceDateBetween} once per student in a loop.
	 */
	@Query("SELECT a.studentId AS studentId, COUNT(a) AS total FROM Attendance a "
			+ "WHERE a.tenantId = :tenantId AND a.studentId IN :studentIds AND a.attendanceDate BETWEEN :from AND :to "
			+ "GROUP BY a.studentId")
	List<StudentAttendanceCount> countByStudentIdsAndTenantIdAndAttendanceDateBetween(
			@Param("studentIds") List<Long> studentIds, @Param("tenantId") Long tenantId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);

	/**
	 * Same as above, additionally filtered by {@code status} — batched alternative to
	 * {@link #countByStudentIdAndTenantIdAndAttendanceDateBetweenAndStatus}.
	 */
	@Query("SELECT a.studentId AS studentId, COUNT(a) AS total FROM Attendance a "
			+ "WHERE a.tenantId = :tenantId AND a.studentId IN :studentIds AND a.attendanceDate BETWEEN :from AND :to "
			+ "AND a.status = :status GROUP BY a.studentId")
	List<StudentAttendanceCount> countByStudentIdsAndTenantIdAndAttendanceDateBetweenAndStatus(
			@Param("studentIds") List<Long> studentIds, @Param("tenantId") Long tenantId,
			@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("status") AttendanceStatus status);

	/**
	 * Batched alternative to calling {@link #existsByClassroomIdAndAttendanceDateAndTenantId} once
	 * per classroom in a loop — the classroom ids (out of any candidate set) that already have
	 * attendance marked on {@code date}.
	 */
	@Query("SELECT DISTINCT a.classroomId FROM Attendance a WHERE a.tenantId = :tenantId AND a.attendanceDate = :date")
	List<Long> findDistinctClassroomIdsMarkedOnDate(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

	interface StudentAttendanceCount {
		Long getStudentId();

		long getTotal();
	}

	// Offline-sync delta pulls — @SQLRestriction("deleted = false") means a soft-deleted row is
	// invisible here, so a delete never surfaces as a tombstone via this query; acceptable for now
	// since AttendanceOfflineSyncHandler's own delete() path already reflects the deletion in its
	// synchronous response, only a later independent delta pull would miss it.
	List<Attendance> findByTenantIdAndUpdatedAtAfter(Long tenantId, Instant updatedAt);
}
