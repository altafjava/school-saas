package com.altafjava.school.application.library;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.library.model.Book;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.model.BookReservation;
import com.altafjava.school.domain.library.model.ReservationStatus;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.BookRepository;
import com.altafjava.school.domain.library.repository.BookReservationRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Gives a copy that has just become free to whoever has waited longest for the title: the copy is
 * held for them for a few days and they are told. With nobody waiting the copy simply stays on the
 * shelf. The hold length is a per-tenant setting.
 */
@Component
@RequiredArgsConstructor
public class ReservationAllocator {

	public static final String HOLD_DAYS_SETTING_KEY = "library.reservation.hold-days";
	private static final int DEFAULT_HOLD_DAYS = 3;

	private final BookReservationRepository bookReservationRepository;
	private final BookCopyRepository bookCopyRepository;
	private final BookRepository bookRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;
	private final TenantSettingOverrideService tenantSettingOverrideService;

	public Optional<BookReservation> allocate(Long tenantId, BookCopy freeCopy, LocalDate today) {
		Optional<BookReservation> next = bookReservationRepository
				.findFirstByBookIdAndStatusAndTenantIdOrderByReservedAtAscIdAsc(freeCopy.getBookId(),
						ReservationStatus.QUEUED, tenantId);
		next.ifPresent(reservation -> {
			freeCopy.hold();
			bookCopyRepository.save(freeCopy);
			reservation.hold(freeCopy.getId(), today.plusDays(resolveHoldDays(tenantId)));
			bookReservationRepository.save(reservation);
			notifyReady(tenantId, reservation);
		});
		return next;
	}

	private int resolveHoldDays(Long tenantId) {
		return tenantSettingOverrideService.get(tenantId, HOLD_DAYS_SETTING_KEY)
				.map(Integer::parseInt)
				.orElse(DEFAULT_HOLD_DAYS);
	}

	private void notifyReady(Long tenantId, BookReservation reservation) {
		String title = bookRepository.findById(reservation.getBookId()).map(Book::getTitle).orElse("your book");
		studentRepository.findByIdAndTenantId(reservation.getStudentId(), tenantId)
				.flatMap(student -> recipientResolver.resolve(tenantId, student))
				.ifPresent(userId -> notificationService.send(SendNotificationCommand.builder()
						.tenantId(tenantId)
						.userId(userId)
						.type(NotificationType.ANNOUNCEMENT)
						.title("Reserved book ready")
						.message("\"" + title + "\" is ready to collect from the library until "
								+ reservation.getHoldExpiresOn() + ".")
						.priority(NotificationPriority.NORMAL)
						.build()));
	}
}
