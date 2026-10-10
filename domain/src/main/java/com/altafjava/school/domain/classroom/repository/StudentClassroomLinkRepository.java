package com.altafjava.school.domain.classroom.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;

public interface StudentClassroomLinkRepository extends JpaRepository<StudentClassroomLink, Long> {

	Optional<StudentClassroomLink> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByStudentIdAndAcademicYearIdAndTenantId(Long studentId, Long academicYearId, Long tenantId);

	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.studentId = :studentId "
			+ "AND l.classroomId = :classroomId")
	Optional<StudentClassroomLink> findByStudentIdAndClassroomId(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId, @Param("classroomId") Long classroomId);

	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.studentId = :studentId "
			+ "AND l.academicYearId = :academicYearId")
	Optional<StudentClassroomLink> findByStudentIdAndAcademicYearId(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId, @Param("academicYearId") Long academicYearId);

	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.classroomId = :classroomId")
	Page<StudentClassroomLink> findByClassroomId(@Param("tenantId") Long tenantId,
			@Param("classroomId") Long classroomId, Pageable pageable);

	// Unpaged variant for internal batch computations (e.g. ReportCardService's class-rank
	// calculation) that need every classmate at once, not a page of them.
	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.classroomId = :classroomId")
	List<StudentClassroomLink> findAllByClassroomId(@Param("tenantId") Long tenantId,
			@Param("classroomId") Long classroomId);

	// Active roster size — @SQLRestriction("deleted = false") on StudentClassroomLink already
	// excludes withdrawn links, so this is the live headcount for capacity enforcement.
	@Query("SELECT COUNT(l) FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.classroomId = :classroomId")
	long countByClassroomId(@Param("tenantId") Long tenantId, @Param("classroomId") Long classroomId);

	@Query("SELECT COUNT(l) > 0 FROM StudentClassroomLink l WHERE l.tenantId = :tenantId "
			+ "AND l.classroomId = :classroomId AND l.studentId IN :studentIds")
	boolean existsByClassroomIdAndStudentIdIn(@Param("tenantId") Long tenantId,
			@Param("classroomId") Long classroomId, @Param("studentIds") Collection<Long> studentIds);

	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.studentId = :studentId")
	List<StudentClassroomLink> findByStudentId(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId);

	/**
	 * Batched alternative to {@link #findByStudentId} for resolving many students' current
	 * classroom at once instead of one query per student in a loop.
	 */
	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.studentId IN :studentIds")
	List<StudentClassroomLink> findByStudentIdIn(@Param("tenantId") Long tenantId,
			@Param("studentIds") List<Long> studentIds);

	// Where the given students sit in one academic year — a student holds at most one classroom per year.
	@Query("SELECT l FROM StudentClassroomLink l WHERE l.tenantId = :tenantId AND l.academicYearId = :academicYearId "
			+ "AND l.studentId IN :studentIds")
	List<StudentClassroomLink> findByStudentIdsAndAcademicYearId(@Param("tenantId") Long tenantId,
			@Param("studentIds") Collection<Long> studentIds, @Param("academicYearId") Long academicYearId);

	// Distinct roster-enrolled student ids tenant-wide — the authoritative "who is actively
	// enrolled" source for jobs that must iterate every student, not just one classroom's.
	@Query("SELECT DISTINCT l.studentId FROM StudentClassroomLink l WHERE l.tenantId = :tenantId")
	List<Long> findDistinctStudentIdsByTenantId(@Param("tenantId") Long tenantId);
}
