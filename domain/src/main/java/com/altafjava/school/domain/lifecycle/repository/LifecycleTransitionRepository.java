package com.altafjava.school.domain.lifecycle.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.lifecycle.model.LifecycleTransition;

public interface LifecycleTransitionRepository extends JpaRepository<LifecycleTransition, Long> {

	// A student's own rows plus every row of the admission they were enrolled from, so the
	// timeline reads continuously from application to today.
	@Query("""
			SELECT t FROM LifecycleTransition t
			WHERE t.tenantId = :tenantId
			  AND (t.studentId = :studentId
			       OR t.admissionId IN (SELECT a.id FROM Admission a
			                            WHERE a.tenantId = :tenantId AND a.enrolledStudentId = :studentId))
			ORDER BY t.createdAt ASC, t.id ASC
			""")
	List<LifecycleTransition> findTimelineForStudent(@Param("tenantId") Long tenantId,
			@Param("studentId") Long studentId);

	List<LifecycleTransition> findAllByAdmissionIdAndTenantIdOrderByCreatedAtAscIdAsc(Long admissionId,
			Long tenantId);
}
