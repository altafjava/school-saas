package com.altafjava.school.domain.payroll.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.payroll.model.SalaryStructure;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

	Optional<SalaryStructure> findByEmployeeIdAndActiveTrueAndTenantId(Long employeeId, Long tenantId);

	Page<SalaryStructure> findAllByEmployeeIdAndTenantId(Long employeeId, Long tenantId, Pageable pageable);

	Optional<SalaryStructure> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
