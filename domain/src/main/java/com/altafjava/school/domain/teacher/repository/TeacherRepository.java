package com.altafjava.school.domain.teacher.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.teacher.model.Teacher;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

	Page<Teacher> findAllByTenantId(Long tenantId, Pageable pageable);

	// Unpaged, batch-context-only overload — for scheduler jobs that must process every teacher in
	// a tenant (e.g. LeaveBalanceAllocationJob), never for a client-facing endpoint.
	List<Teacher> findAllByTenantId(Long tenantId);

	Optional<Teacher> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	List<Teacher> findAllByIdInAndTenantId(List<Long> ids, Long tenantId);

	boolean existsByEmployeeCodeAndTenantId(String employeeCode, Long tenantId);

	boolean existsByIdAndTenantId(Long id, Long tenantId);

	// A teacher who has left cannot be given new classes or timetable slots.
	boolean existsByIdAndTenantIdAndStatus(Long id, Long tenantId, EmployeeStatus status);

	Optional<Teacher> findByIdAndTenantId(Long id, Long tenantId);

	Optional<Teacher> findByUserIdAndTenantId(Long userId, Long tenantId);

	long countByTenantId(Long tenantId);
}
