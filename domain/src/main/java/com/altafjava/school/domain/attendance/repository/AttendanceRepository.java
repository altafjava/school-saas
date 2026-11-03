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
import com.altafjava.school.domain.attendance.model.AttendanceStatusCount;
import com.altafjava.school.domain.attendance.model.AttendanceTally;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

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

	/**
	 * The attendance list. Scope first: every classroom, or only the classrooms the caller teaches plus the
	 * students who are theirs. Every other filter is optional (null matches all) and narrows within that scope.
	 */
	@Query("""
			SELECT a FROM Attendance a
			WHERE a.tenantId = :tenantId
			  AND (:allClassrooms = true OR a.classroomId IN :classroomIds OR a.studentId IN :studentIds)
			  AND (:#{#classroomId == null} = TRUE OR a.classroomId = :classroomId)
			  AND (:#{#studentId == null} = TRUE OR a.studentId = :studentId)
			  AND (:#{#from == null} = TRUE OR a.attendanceDate >= :from)
			  AND (:#{#to == null} = TRUE OR a.attendanceDate <= :to)
			  AND (:#{#status == null} = TRUE OR a.status = :status)
			""")
	Page<Attendance> search(@Param("tenantId") Long tenantId, @Param("allClassrooms") boolean allClassrooms,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("studentIds") Collection<Long> studentIds,
			@Param("classroomId") Long classroomId, @Param("studentId") Long studentId,
			@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("status") AttendanceStatus status,
			Pageable pageable);

	@Query("""
			SELECT a FROM Attendance a
			WHERE a.tenantId = :tenantId AND a.updatedAt > :updatedAt
			  AND (a.classroomId IN :classroomIds OR a.studentId IN :studentIds)
			""")
	List<Attendance> findVisibleUpdatedAfter(@Param("tenantId") Long tenantId, @Param("updatedAt") Instant updatedAt,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("studentIds") Collection<Long> studentIds);

	/**
	 * Batched alternative to calling {@link #existsByClassroomIdAndAttendanceDateAndTenantId} once
	 * per classroom in a loop — the classroom ids (out of any candidate set) that already have
	 * attendance marked on {@code date}.
	 */
	@Query("SELECT DISTINCT a.classroomId FROM Attendance a WHERE a.tenantId = :tenantId AND a.attendanceDate = :date")
	List<Long> findDistinctClassroomIdsMarkedOnDate(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

	// Offline-sync delta pull. Soft-deleted rows are invisible to it, so other devices do not learn
	// of a delete; only the device that deleted sees it, in its own sync response.
	List<Attendance> findByTenantIdAndUpdatedAtAfter(Long tenantId, Instant updatedAt);

	// Which of the given classrooms have a register for the day.
	@Query("SELECT DISTINCT a.classroomId FROM Attendance a WHERE a.tenantId = :tenantId AND a.attendanceDate = :date "
			+ "AND a.classroomId IN :classroomIds")
	List<Long> findClassroomIdsMarkedOn(@Param("tenantId") Long tenantId, @Param("date") LocalDate date,
			@Param("classroomIds") Collection<Long> classroomIds);

	List<Attendance> findAllByTenantIdAndAttendanceDateAndStudentIdIn(Long tenantId, LocalDate attendanceDate,
			Collection<Long> studentIds);

	/** Days per student and status, in one query however many students; {@code excludedDates} are holidays. */
	@Query("""
			SELECT new com.altafjava.school.domain.attendance.model.AttendanceTally(a.studentId, a.status, COUNT(a))
			FROM Attendance a
			WHERE a.tenantId = :tenantId AND a.studentId IN :studentIds AND a.attendanceDate BETWEEN :from AND :to
			  AND (:everyDay = true OR a.attendanceDate NOT IN :excludedDates)
			GROUP BY a.studentId, a.status
			""")
	List<AttendanceTally> tallyByStudent(@Param("tenantId") Long tenantId,
			@Param("studentIds") Collection<Long> studentIds, @Param("from") LocalDate from, @Param("to") LocalDate to,
			@Param("everyDay") boolean everyDay, @Param("excludedDates") Collection<LocalDate> excludedDates);

	@Query("""
			SELECT new com.altafjava.school.domain.attendance.model.AttendanceStatusCount(a.status, COUNT(a))
			FROM Attendance a
			WHERE a.tenantId = :tenantId AND a.attendanceDate BETWEEN :from AND :to
			GROUP BY a.status
			""")
	List<AttendanceStatusCount> countByStatus(@Param("tenantId") Long tenantId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);
}
