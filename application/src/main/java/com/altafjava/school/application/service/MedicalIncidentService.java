package com.altafjava.school.application.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.health.model.MedicalIncident;
import com.altafjava.school.domain.health.repository.MedicalIncidentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * Guardian notification reuses {@link NotificationType#SYSTEM_ALERT} — platform-saas's
 * {@code NotificationType} enum has no medical-incident-specific value, and this codebase must not
 * modify platform-saas to add one. {@code SYSTEM_ALERT} is the closest existing value that does not
 * actively mislabel the incident as something else (e.g. {@code DISCIPLINE_INCIDENT_RECORDED}
 * would present a medical event to a guardian as a disciplinary one). The actual title/message text
 * sent is still "Medical Incident Reported" — only the enum used for notification-preference
 * filtering is generic. Known limitation: a guardian who has muted {@code SYSTEM_ALERT} for
 * unrelated reasons would also miss this notification; a dedicated
 * {@code MEDICAL_INCIDENT_REPORTED} platform enum value is a follow-up outside this module's reach.
 */
@Service
public class MedicalIncidentService {

	private final MedicalIncidentRepository medicalIncidentRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;
	private final ActivityLogService activityLogService;

	public MedicalIncidentService(MedicalIncidentRepository medicalIncidentRepository,
			StudentRepository studentRepository, StudentNotificationRecipientResolver recipientResolver,
			NotificationService notificationService, ActivityLogService activityLogService) {
		this.medicalIncidentRepository = medicalIncidentRepository;
		this.studentRepository = studentRepository;
		this.recipientResolver = recipientResolver;
		this.notificationService = notificationService;
		this.activityLogService = activityLogService;
	}

	@Transactional(readOnly = true)
	public Page<MedicalIncident> listAll(Pageable pageable) {
		return medicalIncidentRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<MedicalIncident> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		return medicalIncidentRepository.findAllByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional
	public MedicalIncident record(String studentPublicId, LocalDateTime occurredAt, String description,
			String treatmentGiven) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		Long recordedByUserId = resolveCurrentUserId();

		MedicalIncident incident = MedicalIncident.record(student.getId(), occurredAt, description, treatmentGiven,
				recordedByUserId);
		MedicalIncident saved = medicalIncidentRepository.save(incident);
		notifyGuardian(tenantId, student, saved);
		// description/treatmentGiven are deliberately excluded from the audit trail — medical PII.
		activityLogService.log(tenantId, "CREATE", "MedicalIncident", String.valueOf(saved.getId()),
				String.valueOf(recordedByUserId), null, null, "Medical incident recorded", null, null);
		return saved;
	}

	private void notifyGuardian(Long tenantId, Student student, MedicalIncident incident) {
		recipientResolver.resolve(tenantId, student).ifPresent(userId -> {
			notificationService.send(SendNotificationCommand.builder()
					.tenantId(tenantId)
					.userId(userId)
					.type(NotificationType.SYSTEM_ALERT)
					.title("Medical Incident Reported")
					.message("A medical incident was recorded for your child.")
					.templateVariables(Map.of(
							"studentName", student.getFirstName() + " " + student.getLastName(),
							"occurredAt", incident.getOccurredAt().toString(),
							"description", incident.getDescription()))
					.priority(NotificationPriority.HIGH)
					.build());
			incident.markGuardianNotified();
			medicalIncidentRepository.save(incident);
		});
	}

	private Long resolveCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot resolve recording user");
	}
}
