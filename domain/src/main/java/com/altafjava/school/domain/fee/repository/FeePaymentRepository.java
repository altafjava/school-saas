package com.altafjava.school.domain.fee.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
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
import com.altafjava.school.domain.fee.model.FeeCollectionRow;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeePaymentStatus;
import com.altafjava.school.domain.fee.model.InvoicePayment;
import com.altafjava.school.domain.fee.model.PaymentMode;
import com.altafjava.school.domain.fee.model.PaymentSource;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

	Page<FeePayment> findAllByTenantId(Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all); {@code paidFrom} is inclusive and {@code paidBefore} exclusive.
	@Query("""
			SELECT fp FROM FeePayment fp
			WHERE fp.tenantId = :tenantId
			  AND (:#{#studentId == null} = TRUE OR fp.studentId = :studentId)
			  AND (:#{#feeStructureId == null} = TRUE OR fp.feeStructureId = :feeStructureId)
			  AND (:#{#paidFrom == null} = TRUE OR fp.paidAt >= :paidFrom)
			  AND (:#{#paidBefore == null} = TRUE OR fp.paidAt < :paidBefore)
			  AND (:#{#paymentSource == null} = TRUE OR fp.paymentSource = :paymentSource)
			  AND (:#{#paymentMode == null} = TRUE OR fp.paymentMode = :paymentMode)
			""")
	Page<FeePayment> search(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("feeStructureId") Long feeStructureId, @Param("paidFrom") LocalDateTime paidFrom,
			@Param("paidBefore") LocalDateTime paidBefore, @Param("paymentSource") PaymentSource paymentSource,
			@Param("paymentMode") PaymentMode paymentMode, Pageable pageable);

	Optional<FeePayment> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	// Row lock that serializes refunds of one payment, so two concurrent requests cannot each pass
	// the "amount still refundable" check and together refund more than was paid.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT fp FROM FeePayment fp WHERE fp.publicId = :publicId AND fp.tenantId = :tenantId")
	Optional<FeePayment> findByPublicIdAndTenantIdForUpdate(@Param("publicId") UUID publicId,
			@Param("tenantId") Long tenantId);

	// Money counts only once it has cleared: every balance and total below leaves bounced cheques out.
	@Query("SELECT fp FROM FeePayment fp WHERE fp.tenantId = :tenantId AND fp.studentId = :studentId "
			+ "AND fp.status = com.altafjava.school.domain.fee.model.FeePaymentStatus.CLEARED")
	List<FeePayment> findClearedByStudentId(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId);

	/**
	 * Batched alternative to {@link #findClearedByStudentId} for computing many students' fee balances at
	 * once instead of one query per student in a loop.
	 */
	@Query("SELECT fp FROM FeePayment fp WHERE fp.tenantId = :tenantId AND fp.studentId IN :studentIds "
			+ "AND fp.status = com.altafjava.school.domain.fee.model.FeePaymentStatus.CLEARED")
	List<FeePayment> findClearedByStudentIdIn(@Param("tenantId") Long tenantId,
			@Param("studentIds") List<Long> studentIds);

	boolean existsByReceiptNumberAndTenantId(String receiptNumber, Long tenantId);

	Optional<FeePayment> findByGatewayChargeReferenceAndTenantId(String gatewayChargeReference, Long tenantId);

	// Campus-level aggregate for the multi-campus rollup report — summed at the DB rather than
	// pulled row-by-row, since a campus can have thousands of payments (see OrganizationRollupService).
	@Query("SELECT COALESCE(SUM(fp.paidAmount), 0) FROM FeePayment fp WHERE fp.tenantId = :tenantId "
			+ "AND fp.status = com.altafjava.school.domain.fee.model.FeePaymentStatus.CLEARED")
	BigDecimal sumClearedAmountByTenantId(@Param("tenantId") Long tenantId);

	@Query("SELECT COUNT(fp) FROM FeePayment fp WHERE fp.tenantId = :tenantId "
			+ "AND fp.status = com.altafjava.school.domain.fee.model.FeePaymentStatus.CLEARED")
	long countClearedByTenantId(@Param("tenantId") Long tenantId);

	// Monthly fee-collection trend (see FeeCollectionTrendDataProvider) — summed at the DB per
	// period rather than pulled row-by-row, same reasoning as sumClearedAmountByTenantId above.
	@Query("SELECT COALESCE(SUM(fp.paidAmount), 0) FROM FeePayment fp WHERE fp.tenantId = :tenantId "
			+ "AND fp.paidAt BETWEEN :from AND :to AND fp.status = com.altafjava.school.domain.fee.model.FeePaymentStatus.CLEARED")
	BigDecimal sumClearedAmountByTenantIdAndPaidAtBetween(@Param("tenantId") Long tenantId,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	@Query("""
			SELECT new com.altafjava.school.domain.fee.model.FeeCollectionRow(
			    CAST(fp.paidAt AS LocalDate), fp.paymentMode,
			    SUM(CASE WHEN fp.status = com.altafjava.school.domain.fee.model.FeePaymentStatus.CLEARED
			        THEN 1L ELSE 0L END),
			    SUM(CASE WHEN fp.status = com.altafjava.school.domain.fee.model.FeePaymentStatus.CLEARED
			        THEN fp.paidAmount ELSE 0 END),
			    SUM(CASE WHEN fp.status = com.altafjava.school.domain.fee.model.FeePaymentStatus.BOUNCED
			        THEN 1L ELSE 0L END),
			    SUM(CASE WHEN fp.status = com.altafjava.school.domain.fee.model.FeePaymentStatus.BOUNCED
			        THEN fp.paidAmount ELSE 0 END))
			FROM FeePayment fp
			WHERE fp.tenantId = :tenantId AND fp.paidAt >= :paidFrom AND fp.paidAt < :paidBefore
			GROUP BY CAST(fp.paidAt AS LocalDate), fp.paymentMode
			ORDER BY CAST(fp.paidAt AS LocalDate), fp.paymentMode
			""")
	List<FeeCollectionRow> collectionByDayAndMode(@Param("tenantId") Long tenantId,
			@Param("paidFrom") LocalDateTime paidFrom, @Param("paidBefore") LocalDateTime paidBefore);

	@Query("""
			SELECT new com.altafjava.school.domain.fee.model.InvoicePayment(fp.invoiceId, COALESCE(SUM(fp.paidAmount), 0))
			FROM FeePayment fp
			WHERE fp.tenantId = :tenantId AND fp.invoiceId IN :invoiceIds AND fp.status = :status
			GROUP BY fp.invoiceId
			""")
	List<InvoicePayment> sumByInvoice(@Param("tenantId") Long tenantId,
			@Param("invoiceIds") Collection<Long> invoiceIds, @Param("status") FeePaymentStatus status);
}
