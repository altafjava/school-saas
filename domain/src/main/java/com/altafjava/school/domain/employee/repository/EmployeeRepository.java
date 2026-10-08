package com.altafjava.school.domain.employee.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	// Either filter may be null to match everything.
	@Query("""
			SELECT e FROM Employee e
			WHERE e.tenantId = :tenantId
			  AND (:category IS NULL OR e.staffCategory = :category)
			  AND (:status IS NULL OR e.status = :status)
			""")
	Page<Employee> search(@Param("tenantId") Long tenantId, @Param("category") StaffCategory category,
			@Param("status") EmployeeStatus status, Pageable pageable);

	// Unpaged, batch-context-only: scheduler jobs (payslips, leave allocation) that must process every
	// current employee in a tenant — never for a client-facing endpoint. Leavers are excluded.
	List<Employee> findAllByTenantIdAndStatus(Long tenantId, EmployeeStatus status);

	// Everyone still employed at some point on or after {@code since}: current staff plus leavers whose
	// last day falls in that period — a leaver is still owed the pay for the days they worked.
	@Query("SELECT e FROM Employee e WHERE e.tenantId = :tenantId AND (e.exitDate IS NULL OR e.exitDate >= :since)")
	List<Employee> findAllEmployedSince(@Param("tenantId") Long tenantId, @Param("since") LocalDate since);

	Optional<Employee> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<Employee> findByIdAndTenantId(Long id, Long tenantId);

	List<Employee> findAllByIdInAndTenantId(List<Long> ids, Long tenantId);

	Optional<Employee> findByUserIdAndTenantId(Long userId, Long tenantId);

	@Query("SELECT e.id FROM Employee e WHERE e.tenantId = :tenantId AND e.departmentId IN :departmentIds")
	List<Long> findIdsInDepartments(@Param("tenantId") Long tenantId,
			@Param("departmentIds") Collection<Long> departmentIds);

	boolean existsByEmployeeCodeAndTenantId(String employeeCode, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	long countByTenantId(Long tenantId);

	long countByTenantIdAndStatus(Long tenantId, EmployeeStatus status);

	long countByTenantIdAndStaffCategoryAndStatus(Long tenantId, StaffCategory staffCategory, EmployeeStatus status);
}
