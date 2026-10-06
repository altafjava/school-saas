package com.altafjava.school.domain.guardian.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.guardian.model.GuardianAuthorizationChange;

public interface GuardianAuthorizationChangeRepository extends JpaRepository<GuardianAuthorizationChange, Long> {

	List<GuardianAuthorizationChange> findAllByStudentIdAndTenantIdOrderByCreatedAtDesc(Long studentId, Long tenantId);
}
