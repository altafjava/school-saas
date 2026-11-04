package com.altafjava.school.application.exam;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.error.AcademicErrorCode;
import com.altafjava.school.domain.exam.event.ExamSchedulePublishedEvent;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import lombok.RequiredArgsConstructor;

/** A classroom's exam timetable: read in date order, and published to its families in one step. */
@Service
@RequiredArgsConstructor
public class ExamTimetableService {

	/** A classroom sits far fewer exams than this in a year; the bound keeps the read finite. */
	private static final int MAX_EXAMS = 500;

	private final ExamRepository examRepository;
	private final ClassroomRepository classroomRepository;
	private final AcademicAccessGuard academicAccessGuard;
	private final ExamScheduleVisibility examScheduleVisibility;
	private final PublicIdLookup publicIdLookup;
	private final EventPublisher eventPublisher;

	@Transactional(readOnly = true)
	public List<Exam> timetable(String classroomPublicId, String termPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = requireClassroom(tenantId, classroomPublicId);
		academicAccessGuard.assertCanViewCoursework(tenantId, classroom.getId());
		ExamScheduleVisibility.Reach reach = examScheduleVisibility.current(tenantId);
		boolean everySchedule = reach.everySchedule() || reach.staffClassroomIds().contains(classroom.getId());
		return examRepository.findTimetable(tenantId, classroom.getId(),
				publicIdLookup.idOrNull(EntityRef.TERM, termPublicId), ExamStatus.CANCELLED, everySchedule,
				PageRequest.of(0, MAX_EXAMS));
	}

	/** Publishes every scheduled exam not yet published in the classroom, optionally only in one term. */
	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "ExamSchedule", details = "Exam schedule published")
	public List<Exam> publish(String classroomPublicId, String termPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = requireClassroom(tenantId, classroomPublicId);
		List<Exam> unpublished = examRepository
				.findTimetable(tenantId, classroom.getId(), publicIdLookup.idOrNull(EntityRef.TERM, termPublicId),
						ExamStatus.CANCELLED, true, PageRequest.of(0, MAX_EXAMS))
				.stream()
				.filter(exam -> exam.getStatus() == ExamStatus.SCHEDULED && !exam.isSchedulePublished())
				.toList();
		if (unpublished.isEmpty()) {
			throw new BusinessException(AcademicErrorCode.EXAM_TIMETABLE_NOTHING_TO_PUBLISH,
					"This classroom has no scheduled exam waiting to be published");
		}
		unpublished.forEach(Exam::publishSchedule);
		List<Exam> saved = examRepository.saveAll(unpublished);
		eventPublisher.publish(new ExamSchedulePublishedEvent(tenantId, classroom.getId(),
				saved.stream().map(Exam::getId).toList()));
		return saved;
	}

	private Classroom requireClassroom(Long tenantId, String classroomPublicId) {
		return classroomRepository.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId));
	}
}
