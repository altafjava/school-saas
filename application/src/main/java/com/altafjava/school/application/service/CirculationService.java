package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.filter.CirculationFilter;
import com.altafjava.school.application.library.ReservationAllocator;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.model.BookCopyStatus;
import com.altafjava.school.domain.library.model.BookReservation;
import com.altafjava.school.domain.library.model.Circulation;
import com.altafjava.school.domain.library.model.ReservationStatus;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.BookReservationRepository;
import com.altafjava.school.domain.library.repository.CirculationRepository;
import com.altafjava.school.domain.library.service.LibraryFineCalculator;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * Checkout/return workflow — due-date window and per-day overdue fine rate are tenant-configurable
 * (via {@code TenantSettingOverrideService}, the same scalar-setting mechanism used elsewhere for
 * genuine per-tenant numeric knobs — unlike Board/Curriculum, a fine rate has no internal structure
 * that would justify a first-class entity).
 */
@Service
public class CirculationService {

	static final String DUE_DAYS_SETTING_KEY = "library.checkout.due-days";
	static final String FINE_PER_DAY_RATE_SETTING_KEY = "library.fine.per-day-rate";
	static final String MAX_RENEWALS_SETTING_KEY = "library.renewal.max-count";
	private static final int DEFAULT_DUE_DAYS = 14;
	private static final int DEFAULT_MAX_RENEWALS = 2;
	private static final BigDecimal DEFAULT_FINE_PER_DAY_RATE = BigDecimal.valueOf(5);

	private final CirculationRepository circulationRepository;
	private final BookCopyRepository bookCopyRepository;
	private final StudentRepository studentRepository;
	private final TenantSettingOverrideService tenantSettingOverrideService;
	private final BookReservationRepository bookReservationRepository;
	private final ReservationAllocator reservationAllocator;
	private final LibraryFineCalculator libraryFineCalculator = new LibraryFineCalculator();
	private final PublicIdLookup publicIdLookup;

	public CirculationService(CirculationRepository circulationRepository, BookCopyRepository bookCopyRepository,
			StudentRepository studentRepository, TenantSettingOverrideService tenantSettingOverrideService,
			BookReservationRepository bookReservationRepository, ReservationAllocator reservationAllocator,
			PublicIdLookup publicIdLookup) {
		this.publicIdLookup = publicIdLookup;
		this.circulationRepository = circulationRepository;
		this.bookCopyRepository = bookCopyRepository;
		this.studentRepository = studentRepository;
		this.tenantSettingOverrideService = tenantSettingOverrideService;
		this.bookReservationRepository = bookReservationRepository;
		this.reservationAllocator = reservationAllocator;
	}

	@Transactional(readOnly = true)
	public Page<Circulation> list(CirculationFilter filter, Pageable pageable) {
		return circulationRepository.search(TenantContext.getCurrentTenantId(),
				publicIdLookup.idOrNull(EntityRef.STUDENT, filter.studentPublicId()),
				publicIdLookup.idOrNull(EntityRef.BOOK, filter.bookPublicId()), filter.returned(),
				filter.dates().from(), filter.dates().to(), pageable);
	}

	@Transactional
	public Circulation checkout(String bookCopyPublicId, String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		BookCopy copy = bookCopyRepository.findByPublicIdAndTenantId(UUID.fromString(bookCopyPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Book copy not found: " + bookCopyPublicId));
		var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));

		checkoutCopy(tenantId, copy, student.getId());
		bookCopyRepository.save(copy);

		LocalDate today = LocalDate.now();
		LocalDate dueDate = today.plusDays(resolveDueDays(tenantId));
		Circulation circulation = Circulation.checkout(copy.getId(), student.getId(), today, dueDate);
		return circulationRepository.save(circulation);
	}

	@Transactional
	public Circulation returnBook(String circulationPublicId, LocalDate returnedAt) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Circulation circulation = circulationRepository
				.findByPublicIdAndTenantId(UUID.fromString(circulationPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Circulation not found: " + circulationPublicId));

		BigDecimal fine = libraryFineCalculator.calculateFine(circulation.getDueDate(), returnedAt,
				resolveFinePerDayRate(tenantId));
		circulation.returnBook(returnedAt, fine);
		Circulation saved = circulationRepository.save(circulation);

		BookCopy copy = bookCopyRepository.findByIdAndTenantId(circulation.getBookCopyId(), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Book copy not found: " + circulation.getBookCopyId()));
		copy.returnCopy();
		bookCopyRepository.save(copy);
		reservationAllocator.allocate(tenantId, copy, LocalDate.now());

		return saved;
	}

	/**
	 * Extends a loan by another loan period, counted from today if the due date is still ahead. A
	 * title other members are queuing for cannot be renewed, or they would wait longer.
	 */
	@Transactional
	public Circulation renew(String circulationPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Circulation circulation = circulationRepository
				.findByPublicIdAndTenantId(UUID.fromString(circulationPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Circulation not found: " + circulationPublicId));
		BookCopy copy = bookCopyRepository.findByIdAndTenantId(circulation.getBookCopyId(), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Book copy not found: " + circulation.getBookCopyId()));
		if (bookReservationRepository.existsQueuedForOthers(tenantId, copy.getBookId(),
				circulation.getStudentId())) {
			throw new BusinessException("Another member is waiting for this book, so it cannot be renewed");
		}
		LocalDate today = LocalDate.now();
		LocalDate base = circulation.getDueDate().isAfter(today) ? circulation.getDueDate() : today;
		circulation.renew(today, base.plusDays(resolveDueDays(tenantId)), resolveMaxRenewals(tenantId));
		return circulationRepository.save(circulation);
	}

	// A copy held for a reservation goes only to the member it was set aside for.
	private void checkoutCopy(Long tenantId, BookCopy copy, Long studentId) {
		if (copy.getStatus() != BookCopyStatus.ON_HOLD) {
			copy.checkout();
			return;
		}
		BookReservation reservation = bookReservationRepository
				.findByHeldCopyIdAndStatusAndTenantId(copy.getId(), ReservationStatus.READY, tenantId)
				.orElseThrow(() -> new BusinessException("Book copy " + copy.getCopyCode() + " is on hold"));
		if (!reservation.getStudentId().equals(studentId)) {
			throw new BusinessException("Book copy " + copy.getCopyCode() + " is being held for another member");
		}
		copy.checkoutHeld();
		reservation.fulfil();
		bookReservationRepository.save(reservation);
	}

	private int resolveDueDays(Long tenantId) {
		return tenantSettingOverrideService.get(tenantId, DUE_DAYS_SETTING_KEY)
				.map(Integer::parseInt)
				.orElse(DEFAULT_DUE_DAYS);
	}

	private int resolveMaxRenewals(Long tenantId) {
		return tenantSettingOverrideService.get(tenantId, MAX_RENEWALS_SETTING_KEY)
				.map(Integer::parseInt)
				.orElse(DEFAULT_MAX_RENEWALS);
	}

	private BigDecimal resolveFinePerDayRate(Long tenantId) {
		return tenantSettingOverrideService.get(tenantId, FINE_PER_DAY_RATE_SETTING_KEY)
				.map(BigDecimal::new)
				.orElse(DEFAULT_FINE_PER_DAY_RATE);
	}
}
