package com.altafjava.school.domain.guardian.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.guardian.model.Guardian;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

	List<Guardian> findAllByIdInAndTenantId(Collection<Long> ids, Long tenantId);

	// Blank q matches everything; pattern comes from LikePattern.contains.
	@Query("""
			SELECT g FROM Guardian g
			WHERE g.tenantId = :tenantId
			  AND (:#{#pattern == null} = TRUE
			       OR LOWER(CONCAT(g.firstName, ' ', g.lastName)) LIKE :pattern ESCAPE '!'
			       OR LOWER(g.email) LIKE :pattern ESCAPE '!')
			""")
	Page<Guardian> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Optional<Guardian> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Guardian> findByUserIdAndTenantId(Long userId, Long tenantId);

	// A "pending, unclaimed" guardian record — created by an admin/saga ahead of the guardian
	// ever logging in — that self-registration can claim by matching email.
	Optional<Guardian> findByEmailAndTenantIdAndUserIdIsNull(String email, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	Optional<Guardian> findByIdAndTenantId(Long id, Long tenantId);

	// A custody-restricted link grants no access to a child, so it does not put a guardian in an audience.
	@Query("""
			SELECT DISTINCT g.userId FROM Guardian g, StudentGuardianLink l, Student s, StudentClassroomLink c
			WHERE l.guardianId = g.id AND s.id = l.studentId AND c.studentId = s.id
			  AND g.tenantId = :tenantId AND l.tenantId = :tenantId AND s.tenantId = :tenantId AND c.tenantId = :tenantId
			  AND g.userId IS NOT NULL AND l.custodyRestricted = false
			  AND s.enrollmentStatus = com.altafjava.school.domain.student.model.EnrollmentStatus.ACTIVE
			  AND c.classroomId IN :classroomIds
			""")
	List<Long> findUserIdsByClassroomIds(@Param("tenantId") Long tenantId,
			@Param("classroomIds") Collection<Long> classroomIds);

	@Query("""
			SELECT DISTINCT g.userId FROM Guardian g, StudentGuardianLink l, Student s
			WHERE l.guardianId = g.id AND s.id = l.studentId
			  AND g.tenantId = :tenantId AND l.tenantId = :tenantId AND s.tenantId = :tenantId
			  AND g.userId IS NOT NULL AND l.custodyRestricted = false
			  AND s.enrollmentStatus = com.altafjava.school.domain.student.model.EnrollmentStatus.ACTIVE
			""")
	List<Long> findUserIdsOfEnrolledStudents(@Param("tenantId") Long tenantId);

	@Query("""
			SELECT g.userId FROM Guardian g
			WHERE g.tenantId = :tenantId AND g.publicId IN :publicIds AND g.userId IS NOT NULL
			""")
	List<Long> findUserIdsByPublicIds(@Param("tenantId") Long tenantId, @Param("publicIds") Collection<UUID> publicIds);
}
