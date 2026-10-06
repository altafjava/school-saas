package com.altafjava.school.domain.guardian.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;

public interface StudentGuardianLinkRepository extends JpaRepository<StudentGuardianLink, Long> {

	Optional<StudentGuardianLink> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByGuardianIdAndStudentIdAndTenantId(Long guardianId, Long studentId, Long tenantId);

	Optional<StudentGuardianLink> findByGuardianIdAndStudentIdAndTenantId(Long guardianId, Long studentId,
			Long tenantId);

	List<StudentGuardianLink> findAllByGuardianIdAndTenantId(Long guardianId, Long tenantId);

	@Query("SELECT l FROM StudentGuardianLink l WHERE l.tenantId = :tenantId AND l.guardianId = :guardianId"
			+ " AND l.custodyRestricted = false")
	Page<StudentGuardianLink> findByGuardianId(@Param("tenantId") Long tenantId,
			@Param("guardianId") Long guardianId, Pageable pageable);

	@Query("SELECT l FROM StudentGuardianLink l WHERE l.tenantId = :tenantId AND l.studentId = :studentId")
	List<StudentGuardianLink> findByStudentId(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId);
}
