package com.altafjava.school.domain.student.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

	Page<Student> findAllByTenantId(Long tenantId, Pageable pageable);

	// Either filter may be null/blank to match everything; pattern comes from LikePattern.contains.
	@Query("""
			SELECT s FROM Student s
			WHERE s.tenantId = :tenantId
			  AND (:status IS NULL OR s.enrollmentStatus = :status)
			  AND (:pattern IS NULL
			       OR LOWER(s.studentCode) LIKE :pattern ESCAPE '!'
			       OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE :pattern ESCAPE '!'
			       OR LOWER(s.email) LIKE :pattern ESCAPE '!')
			""")
	Page<Student> search(@Param("tenantId") Long tenantId, @Param("status") EnrollmentStatus status,
			@Param("pattern") String pattern, Pageable pageable);

	Page<Student> findAllByTenantIdAndEnrollmentStatus(Long tenantId, EnrollmentStatus enrollmentStatus,
			Pageable pageable);

	List<Student> findAllByEnrollmentStatusAndTenantId(EnrollmentStatus enrollmentStatus, Long tenantId);

	List<Student> findAllByIdInAndTenantId(List<Long> ids, Long tenantId);

	List<Student> findAllBySiblingGroupIdAndTenantId(Long siblingGroupId, Long tenantId);

	// Other students linked to any of the given guardians — the people a student might be a sibling of.
	@Query("SELECT DISTINCT s FROM Student s WHERE s.tenantId = :tenantId AND s.id <> :studentId AND s.id IN ("
			+ "SELECT l.studentId FROM StudentGuardianLink l WHERE l.tenantId = :tenantId AND l.guardianId IN ("
			+ "SELECT own.guardianId FROM StudentGuardianLink own WHERE own.tenantId = :tenantId "
			+ "AND own.studentId = :studentId))")
	List<Student> findStudentsSharingAGuardianWith(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId);

	long countByEnrollmentStatusAndTenantId(EnrollmentStatus enrollmentStatus, Long tenantId);

	Optional<Student> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByStudentCodeAndTenantId(String studentCode, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	Optional<Student> findByIdAndTenantId(Long id, Long tenantId);

	@Query("SELECT s FROM Student s WHERE s.tenantId = :tenantId AND s.email = :email")
	Optional<Student> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") Long tenantId);

	Optional<Student> findByUserIdAndTenantId(Long userId, Long tenantId);

	/**
	 * Bulk-fetch for {@code SchoolDataRetentionHandler} — inactive students past a tenant's configured retention
	 * window.
	 */
	List<Student> findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(Long tenantId,
			List<EnrollmentStatus> enrollmentStatuses, Instant cutoff);
}
