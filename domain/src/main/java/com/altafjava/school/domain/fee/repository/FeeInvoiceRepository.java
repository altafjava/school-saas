package com.altafjava.school.domain.fee.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.fee.model.FeeInvoice;
import com.altafjava.school.domain.fee.model.FeeInvoiceLine;
import com.altafjava.school.domain.fee.model.FeeInvoiceStatus;

public interface FeeInvoiceRepository extends JpaRepository<FeeInvoice, Long> {

	Optional<FeeInvoice> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Optional<FeeInvoice> findByIdAndTenantId(Long id, Long tenantId);

	// Every filter is optional (null matches all); the dates match invoices whose period overlaps them.
	@Query("""
			SELECT i FROM FeeInvoice i
			WHERE i.tenantId = :tenantId
			  AND (:#{#studentId == null} = TRUE OR i.studentId = :studentId)
			  AND (:#{#status == null} = TRUE OR i.status = :status)
			  AND (:#{#from == null} = TRUE OR i.periodEnd >= :from)
			  AND (:#{#to == null} = TRUE OR i.periodStart <= :to)
			""")
	Page<FeeInvoice> search(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("status") FeeInvoiceStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to,
			Pageable pageable);

	@Query("""
			SELECT COUNT(i) > 0 FROM FeeInvoice i
			WHERE i.tenantId = :tenantId AND i.studentId = :studentId AND i.status = :status
			  AND i.periodStart = :periodStart AND i.periodEnd = :periodEnd
			""")
	boolean existsForPeriod(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("status") FeeInvoiceStatus status, @Param("periodStart") LocalDate periodStart,
			@Param("periodEnd") LocalDate periodEnd);

	// The charges already billed to a student on live invoices, so a later invoice never repeats one.
	@Query("SELECT l FROM FeeInvoice i JOIN i.lines l WHERE i.tenantId = :tenantId AND i.studentId = :studentId "
			+ "AND i.status = :status")
	List<FeeInvoiceLine> findBilledLines(@Param("tenantId") Long tenantId, @Param("studentId") Long studentId,
			@Param("status") FeeInvoiceStatus status);
}
