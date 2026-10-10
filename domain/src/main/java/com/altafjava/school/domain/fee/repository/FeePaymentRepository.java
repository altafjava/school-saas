package com.altafjava.school.domain.fee.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.PaymentSource;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

	Page<FeePayment> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all); {@code paidFrom} is inclusive and {@code paidBefore} exclusive.
	@Query("""
			SELECT fp FROM FeePayment fp
			WHERE fp.tenantId = :tenantId
			  AND (:studentId IS NULL OR fp.studentId = :studentId)
			  AND (:feeStructureId IS NULL OR fp.feeStructureId = :feeStructureId)
			  AND (:paidFrom IS NULL OR fp.paidAt >= :paidFrom)
			  AND (:paidBefore IS NULL OR fp.paidAt < :paidBefore)
			  AND (:paymentSource IS NULL OR fp.paymentSource = :paymentSource)
			""")
	Page<FeePayment> search(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("feeStructureId") Long feeStructureId, @Param("paidFrom") LocalDateTime paidFrom,
			@Param("paidBefore") LocalDateTime paidBefore, @Param("paymentSource") PaymentSource paymentSource,
			Pageable pageable);

	Optional<FeePayment> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	// Row lock that serializes refunds of one payment, so two concurrent requests cannot each pass
	// the "amount still refundable" check and together refund more than was paid.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT fp FROM FeePayment fp WHERE fp.publicId = :publicId AND fp.tenantId = :tenantId")
	Optional<FeePayment> findByPublicIdAndTenantIdForUpdate(@Param("publicId") UUID publicId,
			@Param("tenantId") Long tenantId);

	@Query("SELECT fp FROM FeePayment fp WHERE fp.tenantId = :tenantId AND fp.studentId = :studentId")
	List<FeePayment> findByStudentId(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId);

	/**
	 * Batched alternative to {@link #findByStudentId} for computing many students' fee balances at
	 * once instead of one query per student in a loop.
	 */
	@Query("SELECT fp FROM FeePayment fp WHERE fp.tenantId = :tenantId AND fp.studentId IN :studentIds")
	List<FeePayment> findByStudentIdIn(@Param("tenantId") Long tenantId, @Param("studentIds") List<Long> studentIds);

	boolean existsByReceiptNumberAndTenantId(String receiptNumber, Long tenantId);

	Optional<FeePayment> findByGatewayChargeReferenceAndTenantId(String gatewayChargeReference, Long tenantId);

	// Campus-level aggregate for the multi-campus rollup report — summed at the DB rather than
	// pulled row-by-row, since a campus can have thousands of payments (see OrganizationRollupService).
	@Query("SELECT COALESCE(SUM(fp.paidAmount), 0) FROM FeePayment fp WHERE fp.tenantId = :tenantId")
	BigDecimal sumPaidAmountByTenantId(@Param("tenantId") Long tenantId);

	long countByTenantId(Long tenantId);

	// Monthly fee-collection trend (see FeeCollectionTrendDataProvider) — summed at the DB per
	// period rather than pulled row-by-row, same reasoning as sumPaidAmountByTenantId above.
	@Query("SELECT COALESCE(SUM(fp.paidAmount), 0) FROM FeePayment fp WHERE fp.tenantId = :tenantId "
			+ "AND fp.paidAt BETWEEN :from AND :to")
	BigDecimal sumPaidAmountByTenantIdAndPaidAtBetween(@Param("tenantId") Long tenantId,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
