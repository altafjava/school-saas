package com.altafjava.school.domain.fee.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.fee.model.FeeStructure;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

	Page<FeeStructure> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<FeeStructure> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	boolean existsByNameAndTenantId(String name, Long tenantId);
}
