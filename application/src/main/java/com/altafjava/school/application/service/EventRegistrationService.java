package com.altafjava.school.application.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.event.model.Event;
import com.altafjava.school.domain.event.model.EventRegistration;
import com.altafjava.school.domain.event.model.EventRegistrationStatus;
import com.altafjava.school.domain.event.repository.EventRegistrationRepository;
import com.altafjava.school.domain.event.repository.EventRepository;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class EventRegistrationService {

	private final EventRegistrationRepository eventRegistrationRepository;
	private final EventRepository eventRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final NotificationService notificationService;

	public EventRegistrationService(EventRegistrationRepository eventRegistrationRepository,
			EventRepository eventRepository, StudentRepository studentRepository,
			StudentNotificationRecipientResolver recipientResolver, NotificationService notificationService) {
		this.eventRegistrationRepository = eventRegistrationRepository;
		this.eventRepository = eventRepository;
		this.studentRepository = studentRepository;
		this.recipientResolver = recipientResolver;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public Page<EventRegistration> listForEvent(String eventPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Event event = eventRepository.findByPublicIdAndTenantId(UUID.fromString(eventPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventPublicId));
		return eventRegistrationRepository.findAllByEventIdAndTenantId(event.getId(), tenantId, pageable);
	}

	@Transactional
	public EventRegistration register(String eventPublicId, String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Event event = eventRepository.findByPublicIdAndTenantId(UUID.fromString(eventPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventPublicId));
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));

		if (eventRegistrationRepository.existsByEventIdAndStudentIdAndTenantIdAndStatus(event.getId(),
				student.getId(), tenantId, EventRegistrationStatus.REGISTERED)) {
			throw new BusinessException("Student " + studentPublicId + " is already registered for this event");
		}
		if (event.getCapacity() != null) {
			long registeredCount = eventRegistrationRepository.countByEventIdAndTenantIdAndStatus(event.getId(),
					tenantId, EventRegistrationStatus.REGISTERED);
			if (registeredCount >= event.getCapacity()) {
				throw new BusinessException("Event " + eventPublicId + " has reached its registration capacity");
			}
		}

		EventRegistration registration = eventRegistrationRepository
				.save(EventRegistration.register(event.getId(), student.getId()));
		notifyStudent(tenantId, student, event);
		return registration;
	}

	@Transactional
	public EventRegistration cancel(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		EventRegistration registration = eventRegistrationRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Event registration not found: " + publicId));
		registration.cancel();
		return eventRegistrationRepository.save(registration);
	}

	@Transactional
	public EventRegistration markAttended(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		EventRegistration registration = eventRegistrationRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Event registration not found: " + publicId));
		registration.markAttended();
		return eventRegistrationRepository.save(registration);
	}

	private void notifyStudent(Long tenantId, Student student, Event event) {
		recipientResolver.resolve(tenantId, student)
				.ifPresent(userId -> notificationService.send(SendNotificationCommand.builder()
						.tenantId(tenantId)
						.userId(userId)
						.type(SchoolNotificationTypes.EVENT_REGISTRATION_CONFIRMED)
						.title("Event Registration Confirmed")
						.message("Registered for " + event.getTitle())
						.templateVariables(Map.of(
								"studentName", student.getFirstName() + " " + student.getLastName(),
								"eventTitle", event.getTitle(),
								"eventDate", event.getEventDate().toString()))
						.priority(NotificationPriority.NORMAL)
						.build()));
	}
}
