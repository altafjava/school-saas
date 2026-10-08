package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.library.ReservationAllocator;
import com.altafjava.school.domain.library.model.Book;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.model.BookCopyStatus;
import com.altafjava.school.domain.library.model.BookReservation;
import com.altafjava.school.domain.library.model.ReservationStatus;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.BookRepository;
import com.altafjava.school.domain.library.repository.BookReservationRepository;
import com.altafjava.school.domain.library.repository.CirculationRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/** Queue for titles with no copy on the shelf; the allocator serves it as copies come back. */
@Service
@RequiredArgsConstructor
public class BookReservationService {

	private final BookReservationRepository bookReservationRepository;
	private final BookRepository bookRepository;
	private final BookCopyRepository bookCopyRepository;
	private final StudentRepository studentRepository;
	private final CirculationRepository circulationRepository;
	private final ReservationAllocator reservationAllocator;

	@Transactional(readOnly = true)
	public Page<BookReservation> list(String bookPublicId, String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if ((bookPublicId == null) == (studentPublicId == null)) {
			throw new BusinessException("Filter reservations by exactly one of bookPublicId or studentPublicId");
		}
		if (bookPublicId != null) {
			return bookReservationRepository.findAllByBookIdAndTenantId(requireBook(tenantId, bookPublicId).getId(),
					tenantId, pageable);
		}
		return bookReservationRepository.findAllByStudentIdAndTenantId(
				requireStudent(tenantId, studentPublicId).getId(), tenantId, pageable);
	}

	@Transactional
	public BookReservation reserve(String bookPublicId, String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Book book = requireBook(tenantId, bookPublicId);
		Student student = requireStudent(tenantId, studentPublicId);
		requireEligible(tenantId, book, student);
		return bookReservationRepository.save(BookReservation.queue(book.getId(), student.getId()));
	}

	@Transactional
	public BookReservation cancel(String reservationPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		BookReservation reservation = requireReservation(tenantId, reservationPublicId);
		Long heldCopyId = reservation.getHeldCopyId();
		boolean holdingACopy = reservation.getStatus() == ReservationStatus.READY;
		reservation.cancel();
		BookReservation saved = bookReservationRepository.save(reservation);
		if (holdingACopy) {
			passOn(tenantId, heldCopyId, LocalDate.now());
		}
		return saved;
	}

	/** Frees copies whose hold ran out uncollected and offers each to the next member in the queue. */
	@Transactional
	public int expireLapsedHolds(Long tenantId, LocalDate today) {
		List<BookReservation> lapsed = bookReservationRepository.findHoldsLapsedBefore(tenantId, today);
		for (BookReservation reservation : lapsed) {
			Long heldCopyId = reservation.getHeldCopyId();
			reservation.expire();
			bookReservationRepository.save(reservation);
			passOn(tenantId, heldCopyId, today);
		}
		return lapsed.size();
	}

	private void passOn(Long tenantId, Long copyId, LocalDate today) {
		BookCopy copy = bookCopyRepository.findByIdAndTenantId(copyId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Book copy not found: " + copyId));
		copy.releaseHold();
		bookCopyRepository.save(copy);
		reservationAllocator.allocate(tenantId, copy, today);
	}

	private void requireEligible(Long tenantId, Book book, Student student) {
		if (!book.isActive()) {
			throw new BusinessException("Book is no longer in the catalog: " + book.getTitle());
		}
		if (student.getEnrollmentStatus() != EnrollmentStatus.ACTIVE) {
			throw new BusinessException("Only an active student can reserve a book");
		}
		if (bookReservationRepository.existsLiveFor(tenantId, book.getId(), student.getId())) {
			throw new BusinessException("The student already has a reservation for this book");
		}
		if (circulationRepository.hasTitleOut(tenantId, book.getId(), student.getId())) {
			throw new BusinessException("The student already has a copy of this book");
		}
		if (bookCopyRepository.existsByBookIdAndStatusAndTenantId(book.getId(), BookCopyStatus.AVAILABLE, tenantId)) {
			throw new BusinessException("A copy is on the shelf — check it out instead of reserving");
		}
		boolean aCopyWillComeBack = bookCopyRepository.existsByBookIdAndStatusAndTenantId(book.getId(),
				BookCopyStatus.CHECKED_OUT, tenantId)
				|| bookCopyRepository.existsByBookIdAndStatusAndTenantId(book.getId(), BookCopyStatus.ON_HOLD,
						tenantId);
		if (!aCopyWillComeBack) {
			throw new BusinessException("No copy of this book can become available");
		}
	}

	private Book requireBook(Long tenantId, String bookPublicId) {
		return bookRepository.findByPublicIdAndTenantId(UUID.fromString(bookPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookPublicId));
	}

	private Student requireStudent(Long tenantId, String studentPublicId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}

	private BookReservation requireReservation(Long tenantId, String reservationPublicId) {
		return bookReservationRepository.findByPublicIdAndTenantId(UUID.fromString(reservationPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationPublicId));
	}
}
