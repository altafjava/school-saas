package com.altafjava.school.domain.payroll.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.payroll.model.Payslip;
import com.altafjava.school.domain.payroll.model.PayslipStatus;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

	boolean existsByEmployeeIdAndPayYearAndPayMonthAndTenantId(Long employeeId, int payYear, int payMonth,
			Long tenantId);

	Page<Payslip> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all).
	@Query("""
			SELECT p FROM Payslip p
			WHERE p.tenantId = :tenantId
			  AND (:employeeId IS NULL OR p.employeeId = :employeeId)
			  AND (:payYear IS NULL OR p.payYear = :payYear)
			  AND (:payMonth IS NULL OR p.payMonth = :payMonth)
			  AND (:status IS NULL OR p.status = :status)
			""")
	Page<Payslip> search(@Param("tenantId") Long tenantId, @Param("employeeId") Long employeeId,
			@Param("payYear") Integer payYear, @Param("payMonth") Integer payMonth,
			@Param("status") PayslipStatus status, Pageable pageable);

	Optional<Payslip> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
