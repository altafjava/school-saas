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
import com.altafjava.school.domain.classroom.model.Classroom;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

	List<Classroom> findAllByIdInAndTenantId(Collection<Long> ids, Long tenantId);

	// Blank q matches everything; pattern comes from LikePattern.contains.
	@Query("""
			SELECT c FROM Classroom c
			WHERE c.tenantId = :tenantId
			  AND (:#{#academicYearId == null} = TRUE OR c.academicYearId = :academicYearId)
			  AND (:#{#grade == null} = TRUE OR c.grade = :grade)
			  AND (:#{#pattern == null} = TRUE
			       OR LOWER(c.classCode) LIKE :pattern ESCAPE '!'
			       OR LOWER(c.grade) LIKE :pattern ESCAPE '!'
			       OR LOWER(c.section) LIKE :pattern ESCAPE '!'
			       OR LOWER(CONCAT(c.grade, ' ', c.section)) LIKE :pattern ESCAPE '!')
			""")
	Page<Classroom> search(@Param("tenantId") Long tenantId, @Param("academicYearId") Long academicYearId,
			@Param("grade") String grade, @Param("pattern") String pattern, Pageable pageable);

	List<Classroom> findAllByTenantId(Long tenantId);

	Optional<Classroom> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Classroom> findByIdAndTenantId(Long id, Long tenantId);

	boolean existsByClassTeacherIdAndTenantId(Long classTeacherId, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	List<Classroom> findAllByClassTeacherIdAndTenantId(Long classTeacherId, Long tenantId);

	List<Classroom> findAllByGradeInAndAcademicYearIdAndTenantId(Collection<String> grades, Long academicYearId,
			Long tenantId);

	List<Classroom> findAllByPublicIdInAndTenantId(Collection<UUID> publicIds, Long tenantId);
}
