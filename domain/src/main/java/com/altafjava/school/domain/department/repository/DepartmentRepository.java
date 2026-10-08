package com.altafjava.school.domain.department.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.department.model.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

	Page<Department> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Department> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Department> findByIdAndTenantId(Long id, Long tenantId);

	@Query("SELECT d.id FROM Department d WHERE d.tenantId = :tenantId AND d.headEmployeeId = :headEmployeeId")
	List<Long> findIdsHeadedBy(@Param("tenantId") Long tenantId, @Param("headEmployeeId") Long headEmployeeId);

	boolean existsByCodeAndTenantId(String code, Long tenantId);

	long countByTenantId(Long tenantId);
}
