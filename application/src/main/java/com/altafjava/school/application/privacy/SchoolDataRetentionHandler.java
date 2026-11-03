package com.altafjava.school.application.privacy;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.core.privacy.DomainRetentionHandler;
import com.altafjava.school.application.messaging.CommunicationDataEraser;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.announcement.model.AnnouncementStatus;
import com.altafjava.school.domain.announcement.repository.AnnouncementRepository;
import com.altafjava.school.domain.messaging.repository.ConversationRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * school-saas's side of the FERPA/COPPA/GDPR-K/DPDP retention-window extension point — see
 * {@link DomainRetentionHandler}, wired via {@code SchoolPlatformConfigurer#domainRetentionHandler()}.
 * Announcements and conversations are swept here too, under their own {@code entityType}. Only
 * students who have already left the school ({@link EnrollmentStatus#WITHDRAWN},
 * {@link EnrollmentStatus#GRADUATED}, {@link EnrollmentStatus#TRANSFERRED}) are ever in scope — an
 * {@code ACTIVE} or {@code SUSPENDED} student's data is still in active use regardless of how a
 * tenant's {@code entityType=STUDENT} policy is configured.
 *
 * <p>
 * Each student is anonymized in its own transaction ({@link TransactionTemplate}, the same
 * pattern {@code CertificateService} uses for the same self-invocation reason —
 * {@code @Transactional} only applies through the Spring proxy, which a private helper method
 * called from within this class would bypass). A batch-wide single transaction would let one
 * student with a data problem (a bad row, a constraint violation) roll back every other, otherwise
 * healthy student in the same tenant's sweep — and since the failing row would still match the
 * same query on every subsequent day's run, it would permanently block that tenant's retention
 * enforcement until someone manually intervened, with no per-tenant signal beyond a log line.
 */
@Slf4j
@Component
public class SchoolDataRetentionHandler implements DomainRetentionHandler {

	private static final String ENTITY_TYPE_STUDENT = "STUDENT";
	private static final String ENTITY_TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
	private static final String ENTITY_TYPE_CONVERSATION = "CONVERSATION";
	private static final String RETENTION_ACTOR = "data-retention-window-elapsed";
	private static final List<AnnouncementStatus> CLOSED_ANNOUNCEMENTS = List.of(AnnouncementStatus.EXPIRED,
			AnnouncementStatus.CANCELLED, AnnouncementStatus.PUBLISHED);
	private static final List<EnrollmentStatus> INACTIVE_STATUSES = List.of(EnrollmentStatus.WITHDRAWN,
			EnrollmentStatus.GRADUATED, EnrollmentStatus.TRANSFERRED);

	private final StudentRepository studentRepository;
	private final AnnouncementRepository announcementRepository;
	private final ConversationRepository conversationRepository;
	private final CommunicationDataEraser communicationDataEraser;
	private final TransactionTemplate transactionTemplate;

	public SchoolDataRetentionHandler(StudentRepository studentRepository,
			AnnouncementRepository announcementRepository, ConversationRepository conversationRepository,
			CommunicationDataEraser communicationDataEraser, PlatformTransactionManager transactionManager) {
		this.studentRepository = studentRepository;
		this.announcementRepository = announcementRepository;
		this.conversationRepository = conversationRepository;
		this.communicationDataEraser = communicationDataEraser;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Override
	public int enforceRetention(Long tenantId, String entityType, Instant cutoff, String deletionPolicy) {
		return switch (entityType) {
			case ENTITY_TYPE_STUDENT -> enforceStudents(tenantId, cutoff, deletionPolicy);
			case ENTITY_TYPE_ANNOUNCEMENT -> enforceAnnouncements(tenantId, cutoff, deletionPolicy);
			case ENTITY_TYPE_CONVERSATION -> enforceConversations(tenantId, cutoff, deletionPolicy);
			default -> {
				log.info("action=retention-entity-type-not-handled tenantId={} entityType={}", tenantId, entityType);
				yield 0;
			}
		};
	}

	// Only announcements that finished circulating: a notice still running stays until it expires.
	private int enforceAnnouncements(Long tenantId, Instant cutoff, String deletionPolicy) {
		return forEachInOwnTransaction(announcementRepository.findClosedBefore(tenantId, CLOSED_ANNOUNCEMENTS, cutoff),
				announcement -> applyToAnnouncement(tenantId, announcement, deletionPolicy));
	}

	private void applyToAnnouncement(Long tenantId, Announcement announcement, String deletionPolicy) {
		switch (deletionPolicy) {
			case "HARD_DELETE" -> communicationDataEraser.deleteAnnouncement(tenantId, announcement);
			case "ANONYMIZE" -> {
				announcement.erasePii();
				announcement.softDelete(RETENTION_ACTOR);
				announcementRepository.save(announcement);
			}
			default -> {
				announcement.softDelete(RETENTION_ACTOR);
				announcementRepository.save(announcement);
			}
		}
	}

	private int enforceConversations(Long tenantId, Instant cutoff, String deletionPolicy) {
		return forEachInOwnTransaction(conversationRepository.findIdleSince(tenantId, cutoff),
				conversation -> {
					if ("HARD_DELETE".equals(deletionPolicy)) {
						communicationDataEraser.deleteConversation(tenantId, conversation);
					} else {
						communicationDataEraser.eraseConversation(tenantId, conversation);
					}
				});
	}

	// One bad row is reported after the rest have committed, as for students below.
	private <T> int forEachInOwnTransaction(List<T> rows, Consumer<T> action) {
		int failed = 0;
		for (T row : rows) {
			try {
				transactionTemplate.executeWithoutResult(status -> action.accept(row));
			} catch (RuntimeException e) {
				failed++;
				log.error("action=retention-row-failed", e);
			}
		}
		if (failed > 0) {
			throw new IllegalStateException(failed + " of " + rows.size() + " retention actions failed");
		}
		return rows.size();
	}

	private int enforceStudents(Long tenantId, Instant cutoff, String deletionPolicy) {
		List<Student> eligible = studentRepository
				.findAllByTenantIdAndEnrollmentStatusInAndEnrollmentStatusChangedAtLessThanEqual(tenantId,
						INACTIVE_STATUSES, cutoff);

		int succeeded = 0;
		int failed = 0;
		for (Student student : eligible) {
			if (anonymizeInOwnTransaction(tenantId, student.getId(), deletionPolicy)) {
				succeeded++;
			} else {
				failed++;
			}
		}

		if (failed > 0) {
			// Thrown after processing every student rather than mid-loop, so the healthy
			// students above already committed in their own transactions — only the count
			// reported back to DataRetentionEnforcementScheduler undercounts on a partial-failure
			// day (it logs this as a failed policy run and moves on); the actual anonymizations
			// are not lost.
			throw new IllegalStateException(
					failed + " of " + eligible.size() + " student retention actions failed for tenant " + tenantId
							+ " — see preceding per-student error logs");
		}
		return succeeded;
	}

	private boolean anonymizeInOwnTransaction(Long tenantId, Long studentId, String deletionPolicy) {
		try {
			transactionTemplate.executeWithoutResult(status -> {
				Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
						.orElseThrow(() -> new IllegalStateException("Student not found: " + studentId));
				applyDeletionPolicy(student, deletionPolicy);
				studentRepository.save(student);
			});
			return true;
		} catch (Exception e) {
			log.error("action=retention-student-failed tenantId={} studentId={}", tenantId, studentId, e);
			return false;
		}
	}

	private void applyDeletionPolicy(Student student, String deletionPolicy) {
		switch (deletionPolicy) {
			case "ANONYMIZE" -> {
				student.erasePii();
				student.softDelete("data-retention-window-elapsed");
			}
			case "SOFT_DELETE" -> student.softDelete("data-retention-window-elapsed");
			default -> throw new UnsupportedOperationException(
					"Student retention does not support deletionPolicy=" + deletionPolicy
							+ " — Student is referenced by attendance/grades/fee records a hard delete would orphan; use ANONYMIZE or SOFT_DELETE");
		}
	}
}
