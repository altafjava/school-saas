package com.altafjava.school.application.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.library.model.Book;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.model.BookCopyStatus;
import com.altafjava.school.domain.library.model.BookReservation;
import com.altafjava.school.domain.library.model.ReservationStatus;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.BookRepository;
import com.altafjava.school.domain.library.repository.BookReservationRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class ReservationAllocatorTest {

	private static final Long TENANT_ID = 1L;
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

	@Mock
	private BookReservationRepository bookReservationRepository;
	@Mock
	private BookCopyRepository bookCopyRepository;
	@Mock
	private BookRepository bookRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private NotificationService notificationService;
	@Mock
	private TenantSettingOverrideService tenantSettingOverrideService;

	private ReservationAllocator allocator;
	private BookCopy copy;

	@BeforeEach
	void setUp() {
		allocator = new ReservationAllocator(bookReservationRepository, bookCopyRepository, bookRepository,
				studentRepository, recipientResolver, notificationService, tenantSettingOverrideService);
		copy = BookCopy.create(7L, "COPY-1");
		copy.setId(70L);
	}

	@Test
	void allocate_withNobodyWaiting_leavesTheCopyOnTheShelf() {
		when(bookReservationRepository.findFirstByBookIdAndStatusAndTenantIdOrderByReservedAtAscIdAsc(7L,
				ReservationStatus.QUEUED, TENANT_ID)).thenReturn(Optional.empty());

		Optional<BookReservation> result = allocator.allocate(TENANT_ID, copy, TODAY);

		assertTrue(result.isEmpty());
		assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
		verify(bookCopyRepository, never()).save(any());
	}

	@Test
	void allocate_withSomeoneWaiting_holdsTheCopyForThemForTheDefaultPeriodAndTellsThem() {
		BookReservation waiting = BookReservation.queue(7L, 11L);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		Book book = Book.create("978", "Dune", "Herbert", "Ace", "Fiction");
		when(bookReservationRepository.findFirstByBookIdAndStatusAndTenantIdOrderByReservedAtAscIdAsc(7L,
				ReservationStatus.QUEUED, TENANT_ID)).thenReturn(Optional.of(waiting));
		when(tenantSettingOverrideService.get(TENANT_ID, ReservationAllocator.HOLD_DAYS_SETTING_KEY))
				.thenReturn(Optional.empty());
		when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
		when(studentRepository.findByIdAndTenantId(11L, TENANT_ID)).thenReturn(Optional.of(student));
		when(recipientResolver.resolve(TENANT_ID, student)).thenReturn(Optional.of(500L));

		Optional<BookReservation> result = allocator.allocate(TENANT_ID, copy, TODAY);

		assertTrue(result.isPresent());
		assertEquals(BookCopyStatus.ON_HOLD, copy.getStatus());
		assertEquals(ReservationStatus.READY, waiting.getStatus());
		assertEquals(70L, waiting.getHeldCopyId());
		assertEquals(TODAY.plusDays(3), waiting.getHoldExpiresOn());
		verify(bookCopyRepository).save(copy);
		verify(bookReservationRepository).save(waiting);
		verify(notificationService).send(any(SendNotificationCommand.class));
	}

	@Test
	void allocate_usesTheTenantsConfiguredHoldPeriod() {
		BookReservation waiting = BookReservation.queue(7L, 11L);
		when(bookReservationRepository.findFirstByBookIdAndStatusAndTenantIdOrderByReservedAtAscIdAsc(7L,
				ReservationStatus.QUEUED, TENANT_ID)).thenReturn(Optional.of(waiting));
		when(tenantSettingOverrideService.get(TENANT_ID, ReservationAllocator.HOLD_DAYS_SETTING_KEY))
				.thenReturn(Optional.of("7"));
		when(bookRepository.findById(7L)).thenReturn(Optional.empty());
		when(studentRepository.findByIdAndTenantId(11L, TENANT_ID)).thenReturn(Optional.empty());

		allocator.allocate(TENANT_ID, copy, TODAY);

		assertEquals(TODAY.plusDays(7), waiting.getHoldExpiresOn());
		verify(notificationService, never()).send(any(SendNotificationCommand.class));
	}
}
