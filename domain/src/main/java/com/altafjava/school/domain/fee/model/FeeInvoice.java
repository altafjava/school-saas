package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.school.domain.error.FeeErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A bill to one student for one period, listing the charges that fall due in it. The amounts are
 * fixed when it is issued; what has been paid against it is worked out from its payments.
 */
@Entity
@Table(name = "fee_invoices")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class FeeInvoice extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// The platform document issuance holding the PDF.
	@Column(name = "document_issuance_id")
	private Long documentIssuanceId;

	@Column(name = "invoice_number", nullable = false, length = 50)
	private String invoiceNumber;

	@Column(name = "period_start", nullable = false)
	private LocalDate periodStart;

	@Column(name = "period_end", nullable = false)
	private LocalDate periodEnd;

	@Column(name = "issue_date", nullable = false)
	private LocalDate issueDate;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	@Column(name = "cancellation_reason", length = 500)
	private String cancellationReason;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private FeeInvoiceStatus status;

	// Part of the live-invoice unique key: 0 while issued, the row's id once cancelled.
	@Builder.Default
	@Column(name = "retired_token", nullable = false)
	private long retiredToken = 0;

	// Batched so a page of invoices loads its lines in a few queries, not one per invoice.
	@Builder.Default
	@BatchSize(size = 50)
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "fee_invoice_lines", joinColumns = @JoinColumn(name = "invoice_id"))
	@OrderColumn(name = "line_number")
	private List<FeeInvoiceLine> lines = new ArrayList<>();

	@PreUpdate
	private void refreshRetiredToken() {
		this.retiredToken = isCancelled() ? getId() : 0;
	}

	public static FeeInvoice issue(Long studentId, String invoiceNumber, InvoicePeriod period, LocalDate issueDate,
			List<FeeInvoiceLine> lines) {
		if (lines.isEmpty()) {
			throw new BusinessException(FeeErrorCode.NOTHING_DUE, "Nothing is due from this student in the period");
		}
		if (period.dueDate().isBefore(issueDate)) {
			throw new BusinessException(FeeErrorCode.INVOICE_DUE_BEFORE_ISSUE,
					"An invoice cannot fall due before it is issued");
		}
		return FeeInvoice.builder()
				.studentId(studentId)
				.invoiceNumber(invoiceNumber)
				.periodStart(period.start())
				.periodEnd(period.end())
				.issueDate(issueDate)
				.dueDate(period.dueDate())
				.totalAmount(lines.stream().map(FeeInvoiceLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
				.status(FeeInvoiceStatus.ISSUED)
				.lines(new ArrayList<>(lines))
				.build();
	}

	public boolean isCancelled() {
		return status == FeeInvoiceStatus.CANCELLED;
	}

	public boolean bills(Long feeStructureId) {
		return lines.stream().anyMatch(line -> line.getFeeStructureId().equals(feeStructureId));
	}

	public void attachDocument(Long documentIssuanceId) {
		this.documentIssuanceId = documentIssuanceId;
	}

	/** The caller makes sure no payment stands against it; a paid invoice is evidence and stays. */
	public void cancel(String reason) {
		if (isCancelled()) {
			throw new BusinessException(FeeErrorCode.INVOICE_ALREADY_CANCELLED, "The invoice is already cancelled");
		}
		this.status = FeeInvoiceStatus.CANCELLED;
		this.cancelledAt = Instant.now();
		this.cancellationReason = reason;
	}

	/** What a payment of {@code paid} leaves of this invoice, and what that makes it. */
	public InvoiceSettlement settlement(BigDecimal paid) {
		if (isCancelled()) {
			return new InvoiceSettlement(paid, BigDecimal.ZERO, InvoiceSettlement.State.CANCELLED);
		}
		BigDecimal outstanding = totalAmount.subtract(paid).max(BigDecimal.ZERO);
		InvoiceSettlement.State state = outstanding.signum() == 0 ? InvoiceSettlement.State.PAID
				: paid.signum() > 0 ? InvoiceSettlement.State.PARTIALLY_PAID : InvoiceSettlement.State.UNPAID;
		return new InvoiceSettlement(paid, outstanding, state);
	}
}
