package com.altafjava.school.domain.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "circulations")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Circulation extends SoftDeletableEntity {

	@Column(name = "book_copy_id", nullable = false)
	private Long bookCopyId;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "checked_out_at", nullable = false)
	private LocalDate checkedOutAt;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "returned_at")
	private LocalDate returnedAt;

	@Column(name = "fine_amount", precision = 10, scale = 2)
	private BigDecimal fineAmount;

	@Column(name = "renewal_count", nullable = false)
	private int renewalCount;

	@Column(name = "last_renewed_at")
	private LocalDate lastRenewedAt;

	public static Circulation checkout(Long bookCopyId, Long studentId, LocalDate checkedOutAt, LocalDate dueDate) {
		return Circulation.builder()
				.bookCopyId(bookCopyId)
				.studentId(studentId)
				.checkedOutAt(checkedOutAt)
				.dueDate(dueDate)
				.build();
	}

	/** A loan can be extended while it is still out and not yet overdue, up to the school's limit. */
	public void renew(LocalDate today, LocalDate newDueDate, int maxRenewals) {
		if (this.returnedAt != null) {
			throw new BusinessException("Cannot renew a book that was returned on " + this.returnedAt);
		}
		if (isOverdue(today)) {
			throw new BusinessException("Cannot renew an overdue book — return it first");
		}
		if (this.renewalCount >= maxRenewals) {
			throw new BusinessException("This loan has already been renewed the maximum " + maxRenewals + " time(s)");
		}
		if (!newDueDate.isAfter(this.dueDate)) {
			throw new BusinessException("Renewal must extend the due date");
		}
		this.dueDate = newDueDate;
		this.renewalCount++;
		this.lastRenewedAt = today;
	}

	public void returnBook(LocalDate returnedAt, BigDecimal fineAmount) {
		if (this.returnedAt != null) {
			throw new BusinessException("Circulation already returned on " + this.returnedAt);
		}
		if (returnedAt.isBefore(this.checkedOutAt)) {
			throw new BusinessException("A book cannot be returned before it was checked out");
		}
		this.returnedAt = returnedAt;
		this.fineAmount = fineAmount;
	}

	public boolean isOverdue(LocalDate asOf) {
		return this.returnedAt == null && asOf.isAfter(this.dueDate);
	}
}
