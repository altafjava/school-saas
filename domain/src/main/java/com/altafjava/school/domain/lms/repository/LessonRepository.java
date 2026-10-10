package com.altafjava.school.domain.lms.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.lms.model.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

	Page<Lesson> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Lesson> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Lesson> findByIdAndTenantId(Long id, Long tenantId);

	/**
	 * Scope first: every classroom, or only the given ones. Every other filter is optional (null matches all) and
	 * narrows within that scope; {@code postedFrom} is inclusive and {@code postedBefore} exclusive.
	 */
	@Query("""
			SELECT l FROM Lesson l
			WHERE l.tenantId = :tenantId
			  AND (:everyClassroom = true OR l.classroomId IN :classroomIds)
			  AND (:classroomId IS NULL OR l.classroomId = :classroomId)
			  AND (:subjectId IS NULL OR l.subjectId = :subjectId)
			  AND (:postedFrom IS NULL OR l.postedAt >= :postedFrom)
			  AND (:postedBefore IS NULL OR l.postedAt < :postedBefore)
			""")
	Page<Lesson> search(@Param("tenantId") Long tenantId, @Param("everyClassroom") boolean everyClassroom,
			@Param("classroomIds") Collection<Long> classroomIds, @Param("classroomId") Long classroomId,
			@Param("subjectId") Long subjectId, @Param("postedFrom") LocalDateTime postedFrom,
			@Param("postedBefore") LocalDateTime postedBefore, Pageable pageable);
}
