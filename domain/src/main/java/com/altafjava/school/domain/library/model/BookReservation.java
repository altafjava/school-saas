package com.altafjava.school.domain.library.model;

import java.time.Instant;
import java.time.LocalDate;
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
import lombok.experimental.SuperBuilder;

/**
 * A member's place in the queue for a title with no copy on the shelf. When a copy comes back the
 * first QUEUED reservation turns READY and holds that copy until {@code holdExpiresOn}.
 */
@Entity
@Table(name = "book_reservations")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class BookReservation extends SoftDeletableEntity {

	// FK to books.id — the title, not a particular copy.
	@Column(name = "book_id", nullable = false)
	private Long bookId;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "reserved_at", nullable = false)
	private Instant reservedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ReservationStatus status;

	// FK to book_copies.id — set while READY.
	@Column(name = "held_copy_id")
	private Long heldCopyId;

	@Column(name = "hold_expires_on")
	private LocalDate holdExpiresOn;

	@Column(name = "closed_at")
	private Instant closedAt;

	public static BookReservation queue(Long bookId, Long studentId) {
		return BookReservation.builder()
				.bookId(bookId)
				.studentId(studentId)
				.reservedAt(Instant.now())
				.status(ReservationStatus.QUEUED)
				.build();
	}

	public boolean isLive() {
		return status == ReservationStatus.QUEUED || status == ReservationStatus.READY;
	}

	public void hold(Long copyId, LocalDate holdExpiresOn) {
		requireStatus(ReservationStatus.QUEUED, "hold a copy for");
		this.status = ReservationStatus.READY;
		this.heldCopyId = copyId;
		this.holdExpiresOn = holdExpiresOn;
	}

	public boolean holdHasLapsed(LocalDate today) {
		return status == ReservationStatus.READY && today.isAfter(holdExpiresOn);
	}

	public void fulfil() {
		requireStatus(ReservationStatus.READY, "fulfil");
		close(ReservationStatus.FULFILLED);
	}

	public void cancel() {
		if (!isLive()) {
			throw new BusinessException("Cannot cancel a reservation that is " + status);
		}
		close(ReservationStatus.CANCELLED);
	}

	public void expire() {
		requireStatus(ReservationStatus.READY, "expire");
		close(ReservationStatus.EXPIRED);
	}

	private void close(ReservationStatus closedStatus) {
		this.status = closedStatus;
		this.closedAt = Instant.now();
	}

	private void requireStatus(ReservationStatus required, String action) {
		if (status != required) {
			throw new BusinessException("Cannot " + action + " a reservation that is " + status);
		}
	}
}
