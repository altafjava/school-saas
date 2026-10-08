package com.altafjava.school.domain.student.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.student.model.SiblingGroup;

public interface SiblingGroupRepository extends JpaRepository<SiblingGroup, Long> {

	Optional<SiblingGroup> findByIdAndTenantId(Long id, Long tenantId);
}
