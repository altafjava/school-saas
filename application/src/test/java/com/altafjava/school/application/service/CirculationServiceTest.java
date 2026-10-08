package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.library.ReservationAllocator;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.model.BookCopyStatus;
import com.altafjava.school.domain.library.model.BookReservation;
import com.altafjava.school.domain.library.model.Circulation;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.BookReservationRepository;
import com.altafjava.school.domain.library.repository.CirculationRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class CirculationServiceTest {

	private static final UUID COPY_PUBLIC_ID = UUID.randomUUID();
	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private CirculationRepository circulationRepository;
	@Mock
	private BookCopyRepository bookCopyRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TenantSettingOverrideService tenantSettingOverrideService;
	@Mock
	private BookReservationRepository bookReservationRepository;
	@Mock
	private ReservationAllocator reservationAllocator;

	private CirculationService circulationService;

	@BeforeEach
	void setUp() {
		circulationService = new CirculationService(circulationRepository, bookCopyRepository, studentRepository,
				tenantSettingOverrideService, bookReservationRepository, reservationAllocator);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private BookCopy copyWithId(long id) {
		BookCopy copy = BookCopy.create(1L, "COPY-1");
		copy.setId(id);
		return copy;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void checkout_withNoTenantSetting_usesDefaultDueDays() {
		BookCopy copy = copyWithId(5L);
		when(bookCopyRepository.findByPublicIdAndTenantId(COPY_PUBLIC_ID, 1L)).thenReturn(Optional.of(copy));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(tenantSettingOverrideService.get(1L, CirculationService.DUE_DAYS_SETTING_KEY))
				.thenReturn(Optional.empty());
		when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(inv -> inv.getArgument(0));
		when(circulationRepository.save(any(Circulation.class))).thenAnswer(inv -> inv.getArgument(0));

		Circulation circulation = circulationService.checkout(COPY_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString());

		assertEquals(BookCopyStatus.CHECKED_OUT, copy.getStatus());
		assertEquals(circulation.getCheckedOutAt().plusDays(14), circulation.getDueDate());
	}

	@Test
	void checkout_withTenantConfiguredDueDays_usesConfiguredValue() {
		BookCopy copy = copyWithId(5L);
		when(bookCopyRepository.findByPublicIdAndTenantId(COPY_PUBLIC_ID, 1L)).thenReturn(Optional.of(copy));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(tenantSettingOverrideService.get(1L, CirculationService.DUE_DAYS_SETTING_KEY))
				.thenReturn(Optional.of("21"));
		when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(inv -> inv.getArgument(0));
		when(circulationRepository.save(any(Circulation.class))).thenAnswer(inv -> inv.getArgument(0));

		Circulation circulation = circulationService.checkout(COPY_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString());

		assertEquals(circulation.getCheckedOutAt().plusDays(21), circulation.getDueDate());
	}

	@Test
	void returnBook_overdue_calculatesFineUsingConfiguredRate() {
		UUID circulationPublicId = UUID.randomUUID();
		Circulation circulation = Circulation.checkout(5L, 10L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15));
		BookCopy copy = copyWithId(5L);
		copy.checkout();
		when(circulationRepository.findByPublicIdAndTenantId(circulationPublicId, 1L))
				.thenReturn(Optional.of(circulation));
		when(tenantSettingOverrideService.get(1L, CirculationService.FINE_PER_DAY_RATE_SETTING_KEY))
				.thenReturn(Optional.of("10"));
		when(circulationRepository.save(any(Circulation.class))).thenAnswer(inv -> inv.getArgument(0));
		when(bookCopyRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(copy));
		when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(inv -> inv.getArgument(0));

		Circulation returned = circulationService.returnBook(circulationPublicId.toString(),
				LocalDate.of(2026, 4, 20));

		assertEquals(0, BigDecimal.valueOf(50).compareTo(returned.getFineAmount()));
		assertEquals(BookCopyStatus.AVAILABLE, copy.getStatus());
		verify(reservationAllocator).allocate(1L, copy, LocalDate.now());
	}

	private Circulation loanDueIn(int days, UUID publicId) {
		Circulation circulation = Circulation.checkout(5L, 10L, LocalDate.now().minusDays(7),
				LocalDate.now().plusDays(days));
		circulation.setPublicId(publicId);
		return circulation;
	}

	private void stubLoan(UUID publicId, Circulation circulation) {
		when(circulationRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(circulation));
		when(bookCopyRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(copyWithId(5L)));
	}

	@Test
	void renew_extendsTheDueDateByOneLoanPeriodFromTheCurrentDueDate() {
		UUID publicId = UUID.randomUUID();
		Circulation circulation = loanDueIn(3, publicId);
		stubLoan(publicId, circulation);
		when(bookReservationRepository.existsQueuedForOthers(1L, 1L, 10L)).thenReturn(false);
		when(tenantSettingOverrideService.get(1L, CirculationService.DUE_DAYS_SETTING_KEY))
				.thenReturn(Optional.empty());
		when(tenantSettingOverrideService.get(1L, CirculationService.MAX_RENEWALS_SETTING_KEY))
				.thenReturn(Optional.empty());
		when(circulationRepository.save(any(Circulation.class))).thenAnswer(inv -> inv.getArgument(0));

		Circulation renewed = circulationService.renew(publicId.toString());

		assertEquals(LocalDate.now().plusDays(3 + 14), renewed.getDueDate());
		assertEquals(1, renewed.getRenewalCount());
	}

	@Test
	void renew_beyondTheConfiguredMaximum_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Circulation circulation = loanDueIn(3, publicId);
		stubLoan(publicId, circulation);
		when(tenantSettingOverrideService.get(1L, CirculationService.DUE_DAYS_SETTING_KEY))
				.thenReturn(Optional.empty());
		when(tenantSettingOverrideService.get(1L, CirculationService.MAX_RENEWALS_SETTING_KEY))
				.thenReturn(Optional.of("1"));
		when(circulationRepository.save(any(Circulation.class))).thenAnswer(inv -> inv.getArgument(0));
		circulationService.renew(publicId.toString());

		assertThrows(BusinessException.class, () -> circulationService.renew(publicId.toString()));
	}

	@Test
	void renew_whenAnotherMemberIsQueuingForTheTitle_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		stubLoan(publicId, loanDueIn(3, publicId));
		when(bookReservationRepository.existsQueuedForOthers(1L, 1L, 10L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> circulationService.renew(publicId.toString()));
		verify(circulationRepository, never()).save(any());
	}

	private void stubCheckoutLookups(BookCopy copy) {
		when(bookCopyRepository.findByPublicIdAndTenantId(COPY_PUBLIC_ID, 1L)).thenReturn(Optional.of(copy));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
	}

	@Test
	void checkout_ofACopyHeldForThisMember_fulfilsTheReservation() {
		BookCopy copy = copyWithId(5L);
		copy.hold();
		BookReservation reservation = BookReservation.queue(1L, 10L);
		reservation.hold(5L, LocalDate.now().plusDays(3));
		stubCheckoutLookups(copy);
		when(bookReservationRepository.findByHeldCopyIdAndStatusAndTenantId(5L,
				com.altafjava.school.domain.library.model.ReservationStatus.READY, 1L))
				.thenReturn(Optional.of(reservation));
		when(tenantSettingOverrideService.get(1L, CirculationService.DUE_DAYS_SETTING_KEY))
				.thenReturn(Optional.empty());
		when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(inv -> inv.getArgument(0));
		when(circulationRepository.save(any(Circulation.class))).thenAnswer(inv -> inv.getArgument(0));

		circulationService.checkout(COPY_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString());

		assertEquals(BookCopyStatus.CHECKED_OUT, copy.getStatus());
		assertEquals(com.altafjava.school.domain.library.model.ReservationStatus.FULFILLED, reservation.getStatus());
	}

	@Test
	void checkout_ofACopyHeldForSomeoneElse_throwsBusinessException() {
		BookCopy copy = copyWithId(5L);
		copy.hold();
		BookReservation reservation = BookReservation.queue(1L, 99L);
		reservation.hold(5L, LocalDate.now().plusDays(3));
		stubCheckoutLookups(copy);
		when(bookReservationRepository.findByHeldCopyIdAndStatusAndTenantId(5L,
				com.altafjava.school.domain.library.model.ReservationStatus.READY, 1L))
				.thenReturn(Optional.of(reservation));

		assertThrows(BusinessException.class,
				() -> circulationService.checkout(COPY_PUBLIC_ID.toString(), STUDENT_PUBLIC_ID.toString()));

		assertEquals(BookCopyStatus.ON_HOLD, copy.getStatus());
		verify(circulationRepository, never()).save(any());
	}
}
