package com.altafjava.school.domain.admission.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.model.AdmissionStatus;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

	// Blank q matches everything; pattern comes from LikePattern.contains.
	@Query("""
			SELECT a FROM Admission a
			WHERE a.tenantId = :tenantId
			  AND (:status IS NULL OR a.status = :status)
			  AND (:pattern IS NULL
			       OR LOWER(CONCAT(a.applicantFirstName, ' ', a.applicantLastName)) LIKE :pattern ESCAPE '!'
			       OR LOWER(CONCAT(a.guardianFirstName, ' ', a.guardianLastName)) LIKE :pattern ESCAPE '!'
			       OR LOWER(a.guardianEmail) LIKE :pattern ESCAPE '!'
			       OR LOWER(a.appliedGrade) LIKE :pattern ESCAPE '!')
			""")
	Page<Admission> search(@Param("tenantId") Long tenantId, @Param("status") AdmissionStatus status,
			@Param("pattern") String pattern, Pageable pageable);

	Page<Admission> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Admission> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Admission> findByIdAndTenantId(Long id, Long tenantId);

	Optional<Admission> findByEnrollmentSagaIdAndTenantId(UUID sagaId, Long tenantId);

	// Merit-list generation input: every scored, still-undecided applicant for one grade cohort.
	List<Admission> findAllByTenantIdAndAppliedGradeAndStatusAndEntranceTestScoreIsNotNull(Long tenantId,
			String appliedGrade, AdmissionStatus status);
}
