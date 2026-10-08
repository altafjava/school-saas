package com.altafjava.school.domain.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "book_copies")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BookCopy extends SoftDeletableEntity {

	@Column(name = "book_id", nullable = false)
	private Long bookId;

	@Column(name = "copy_code", nullable = false, length = 50)
	private String copyCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private BookCopyStatus status;

	public static BookCopy create(Long bookId, String copyCode) {
		return BookCopy.builder()
				.bookId(bookId)
				.copyCode(copyCode)
				.status(BookCopyStatus.AVAILABLE)
				.build();
	}

	public void checkout() {
		if (this.status != BookCopyStatus.AVAILABLE) {
			throw new BusinessException("Book copy " + copyCode + " is not available (status " + status + ")");
		}
		this.status = BookCopyStatus.CHECKED_OUT;
	}

	/** The reserving member picks up the copy that was set aside for them. */
	public void checkoutHeld() {
		if (this.status != BookCopyStatus.ON_HOLD) {
			throw new BusinessException("Book copy " + copyCode + " is not on hold (status " + status + ")");
		}
		this.status = BookCopyStatus.CHECKED_OUT;
	}

	public void hold() {
		if (this.status != BookCopyStatus.AVAILABLE) {
			throw new BusinessException("Book copy " + copyCode + " cannot be held (status " + status + ")");
		}
		this.status = BookCopyStatus.ON_HOLD;
	}

	public void releaseHold() {
		if (this.status != BookCopyStatus.ON_HOLD) {
			throw new BusinessException("Book copy " + copyCode + " is not on hold (status " + status + ")");
		}
		this.status = BookCopyStatus.AVAILABLE;
	}

	public void returnCopy() {
		if (this.status != BookCopyStatus.CHECKED_OUT) {
			throw new BusinessException("Book copy " + copyCode + " is not checked out (status " + status + ")");
		}
		this.status = BookCopyStatus.AVAILABLE;
	}

	public void markLost() {
		this.status = BookCopyStatus.LOST;
	}

	public void markDamaged() {
		this.status = BookCopyStatus.DAMAGED;
	}
}
