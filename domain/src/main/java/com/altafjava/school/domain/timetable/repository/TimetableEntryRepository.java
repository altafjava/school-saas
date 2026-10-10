package com.altafjava.school.domain.timetable.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.timetable.model.TimetableEntry;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

	Page<TimetableEntry> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all).
	@Query("""
			SELECT t FROM TimetableEntry t
			WHERE t.tenantId = :tenantId
			  AND (:classroomId IS NULL OR t.classroomId = :classroomId)
			  AND (:teacherId IS NULL OR t.teacherId = :teacherId)
			  AND (:subjectId IS NULL OR t.subjectId = :subjectId)
			  AND (:dayOfWeek IS NULL OR t.dayOfWeek = :dayOfWeek)
			""")
	Page<TimetableEntry> search(@Param("tenantId") Long tenantId, @Param("classroomId") Long classroomId,
			@Param("teacherId") Long teacherId, @Param("subjectId") Long subjectId,
			@Param("dayOfWeek") DayOfWeek dayOfWeek, Pageable pageable);

	Optional<TimetableEntry> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<TimetableEntry> findByIdAndTenantId(Long id, Long tenantId);

	List<TimetableEntry> findAllByTenantIdAndClassroomId(Long tenantId, Long classroomId);

	List<TimetableEntry> findAllByTenantIdAndTeacherId(Long tenantId, Long teacherId);

	boolean existsByTenantIdAndDayOfWeekAndPeriodIdAndClassroomId(Long tenantId, DayOfWeek dayOfWeek, Long periodId,
			Long classroomId);

	List<TimetableEntry> findAllByTenantIdAndDayOfWeek(Long tenantId, DayOfWeek dayOfWeek);

	List<TimetableEntry> findAllByTenantIdAndDayOfWeekAndPeriodId(Long tenantId, DayOfWeek dayOfWeek, Long periodId);

	List<TimetableEntry> findAllByIdInAndTenantId(List<Long> ids, Long tenantId);

	boolean existsByTenantIdAndDayOfWeekAndPeriodIdAndTeacherId(Long tenantId, DayOfWeek dayOfWeek, Long periodId,
			Long teacherId);
}
