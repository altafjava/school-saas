package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
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
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class BookReservationServiceTest {

	private static final Long TENANT_ID = 1L;
	private static final UUID BOOK_PUBLIC_ID = UUID.randomUUID();
	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

	@Mock
	private BookReservationRepository bookReservationRepository;
	@Mock
	private BookRepository bookRepository;
	@Mock
	private BookCopyRepository bookCopyRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private CirculationRepository circulationRepository;
	@Mock
	private ReservationAllocator reservationAllocator;

	private BookReservationService service;
	private Book book;
	private Student student;

	@BeforeEach
	void setUp() {
		service = new BookReservationService(bookReservationRepository, bookRepository, bookCopyRepository,
				studentRepository, circulationRepository, reservationAllocator);
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, null, TenantType.SHARED);
		book = Book.create("978", "Dune", "Herbert", "Ace", "Fiction");
		book.setId(7L);
		student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(11L);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private void stubReservable() {
		when(bookRepository.findByPublicIdAndTenantId(BOOK_PUBLIC_ID, TENANT_ID)).thenReturn(Optional.of(book));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, TENANT_ID))
				.thenReturn(Optional.of(student));
	}

	private void stubNoCopyOnShelfButOneOut() {
		when(bookCopyRepository.existsByBookIdAndStatusAndTenantId(7L, BookCopyStatus.AVAILABLE, TENANT_ID))
				.thenReturn(false);
		when(bookCopyRepository.existsByBookIdAndStatusAndTenantId(7L, BookCopyStatus.CHECKED_OUT, TENANT_ID))
				.thenReturn(true);
	}

	@Test
	void reserve_whenEveryCopyIsOut_queuesTheStudent() {
		stubReservable();
		stubNoCopyOnShelfButOneOut();
		when(bookReservationRepository.save(any(BookReservation.class))).thenAnswer(inv -> inv.getArgument(0));

		BookReservation reservation = service.reserve(BOOK_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString());

		assertEquals(ReservationStatus.QUEUED, reservation.getStatus());
		assertEquals(11L, reservation.getStudentId());
	}

	@Test
	void reserve_whenACopyIsOnTheShelf_throwsBusinessException() {
		stubReservable();
		when(bookCopyRepository.existsByBookIdAndStatusAndTenantId(7L, BookCopyStatus.AVAILABLE, TENANT_ID))
				.thenReturn(true);

		assertThrows(BusinessException.class,
				() -> service.reserve(BOOK_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));
		verify(bookReservationRepository, never()).save(any());
	}

	@Test
	void reserve_whenTheStudentAlreadyHasALiveReservation_throwsBusinessException() {
		stubReservable();
		when(bookReservationRepository.existsLiveFor(TENANT_ID, 7L, 11L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> service.reserve(BOOK_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));
	}

	@Test
	void reserve_whenTheStudentAlreadyHasTheTitleOut_throwsBusinessException() {
		stubReservable();
		when(circulationRepository.hasTitleOut(TENANT_ID, 7L, 11L)).thenReturn(true);

		assertThrows(BusinessException.class,
				() -> service.reserve(BOOK_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));
	}

	@Test
	void reserve_whenNoCopyCanEverComeBack_throwsBusinessException() {
		stubReservable();
		when(bookCopyRepository.existsByBookIdAndStatusAndTenantId(any(), any(), any())).thenReturn(false);

		assertThrows(BusinessException.class,
				() -> service.reserve(BOOK_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));
	}

	@Test
	void reserve_forAWithdrawnStudent_throwsBusinessException() {
		student.withdraw();
		stubReservable();

		assertThrows(BusinessException.class,
				() -> service.reserve(BOOK_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));
	}

	@Test
	void cancel_aWaitingReservation_closesItWithoutTouchingAnyCopy() {
		UUID publicId = UUID.randomUUID();
		BookReservation waiting = BookReservation.queue(7L, 11L);
		when(bookReservationRepository.findByPublicIdAndTenantId(publicId, TENANT_ID))
				.thenReturn(Optional.of(waiting));
		when(bookReservationRepository.save(any(BookReservation.class))).thenAnswer(inv -> inv.getArgument(0));

		BookReservation cancelled = service.cancel(publicId.toString());

		assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
		verify(bookCopyRepository, never()).save(any());
	}

	@Test
	void cancel_aReservationHoldingACopy_releasesItAndOffersItToTheNextInLine() {
		UUID publicId = UUID.randomUUID();
		BookReservation holding = BookReservation.queue(7L, 11L);
		holding.hold(70L, TODAY.plusDays(3));
		BookCopy copy = BookCopy.create(7L, "COPY-1");
		copy.setId(70L);
		copy.hold();
		when(bookReservationRepository.findByPublicIdAndTenantId(publicId, TENANT_ID))
				.thenReturn(Optional.of(holding));
		when(bookReservationRepository.save(any(BookReservation.class))).thenAnswer(inv -> inv.getArgument(0));
		when(bookCopyRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(copy));

		service.cancel(publicId.toString());

		assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
		verify(reservationAllocator).allocate(TENANT_ID, copy, LocalDate.now());
	}

	@Test
	void expireLapsedHolds_closesEachLapsedHoldAndPassesItsCopyOn() {
		BookReservation lapsed = BookReservation.queue(7L, 11L);
		lapsed.hold(70L, TODAY.minusDays(1));
		BookCopy copy = BookCopy.create(7L, "COPY-1");
		copy.setId(70L);
		copy.hold();
		when(bookReservationRepository.findHoldsLapsedBefore(TENANT_ID, TODAY)).thenReturn(List.of(lapsed));
		when(bookCopyRepository.findByIdAndTenantId(70L, TENANT_ID)).thenReturn(Optional.of(copy));

		int expired = service.expireLapsedHolds(TENANT_ID, TODAY);

		assertEquals(1, expired);
		assertEquals(ReservationStatus.EXPIRED, lapsed.getStatus());
		assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
		verify(reservationAllocator).allocate(TENANT_ID, copy, TODAY);
	}

	@Test
	void list_withNeitherOrBothFilters_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> service.list(null, null, PageRequest.of(0, 20)));
		assertThrows(BusinessException.class,
				() -> service.list(BOOK_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString(), PageRequest.of(0, 20)));
	}
}
