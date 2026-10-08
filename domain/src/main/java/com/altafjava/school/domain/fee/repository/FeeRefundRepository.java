package com.altafjava.school.domain.fee.repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.fee.model.FeeRefund;

public interface FeeRefundRepository extends JpaRepository<FeeRefund, Long> {

	List<FeeRefund> findAllByFeePaymentIdAndTenantIdOrderByCreatedAtDesc(Long feePaymentId, Long tenantId);

	List<FeeRefund> findAllByStudentIdAndTenantIdOrderByCreatedAtDesc(Long studentId, Long tenantId);

	boolean existsByCreditNoteNumberAndTenantId(String creditNoteNumber, Long tenantId);

	// Pending counts as reserved: it must not be refunded a second time while the gateway call is open.
	@Query("SELECT COALESCE(SUM(r.amount), 0) FROM FeeRefund r WHERE r.tenantId = :tenantId "
			+ "AND r.feePaymentId = :feePaymentId AND r.status <> com.altafjava.school.domain.fee.model.RefundStatus.FAILED")
	BigDecimal sumReservedByPayment(@Param("tenantId") Long tenantId, @Param("feePaymentId") Long feePaymentId);

	@Query("SELECT r FROM FeeRefund r WHERE r.tenantId = :tenantId AND r.studentId IN :studentIds "
			+ "AND r.status = com.altafjava.school.domain.fee.model.RefundStatus.COMPLETED")
	List<FeeRefund> findCompletedByStudentIdIn(@Param("tenantId") Long tenantId,
			@Param("studentIds") List<Long> studentIds);

	@Query("SELECT COALESCE(SUM(r.amount), 0) FROM FeeRefund r WHERE r.tenantId = :tenantId "
			+ "AND r.status = com.altafjava.school.domain.fee.model.RefundStatus.COMPLETED")
	BigDecimal sumCompletedByTenantId(@Param("tenantId") Long tenantId);
}
