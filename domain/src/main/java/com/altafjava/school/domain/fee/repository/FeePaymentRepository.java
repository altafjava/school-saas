package com.altafjava.school.domain.fee.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.fee.model.FeePayment;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

	Page<FeePayment> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<FeePayment> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT fp FROM FeePayment fp WHERE fp.tenantId = :tenantId AND fp.studentId = :studentId")
	List<FeePayment> findByStudentId(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId);

	boolean existsByReceiptNumberAndTenantId(String receiptNumber, Long tenantId);
}
