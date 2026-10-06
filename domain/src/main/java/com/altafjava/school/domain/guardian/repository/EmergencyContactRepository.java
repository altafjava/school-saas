package com.altafjava.school.domain.guardian.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.guardian.model.EmergencyContact;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

	Optional<EmergencyContact> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	List<EmergencyContact> findAllByStudentIdAndTenantIdOrderByPriorityAsc(Long studentId, Long tenantId);
}
