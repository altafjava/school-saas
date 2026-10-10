package com.altafjava.school.domain.lms.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.lms.model.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

	Page<Assignment> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Assignment> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Assignment> findByIdAndTenantId(Long id, Long tenantId);

	/**
	 * Scope first: every classroom, or only the given ones. Every other filter is optional (null matches all) and
	 * narrows within that scope; the dates bound the due date, inclusive.
	 */
	@Query("""
			SELECT a FROM Assignment a
			WHERE a.tenantId = :tenantId
			  AND (:everyClassroom = true OR a.classroomId IN :classroomIds)
			  AND (:classroomId IS NULL OR a.classroomId = :classroomId)
			  AND (:subjectId IS NULL OR a.subjectId = :subjectId)
			  AND (:from IS NULL OR a.dueDate >= :from)
			  AND (:to IS NULL OR a.dueDate <= :to)
			""")
	Page<Assignment> search(@Param("tenantId") Long tenantId, @Param("everyClassroom") boolean everyClassroom,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("classroomId") Long classroomId,
			@Param("subjectId") Long subjectId, @Param("from") LocalDate from, @Param("to") LocalDate to,
			Pageable pageable);
}
