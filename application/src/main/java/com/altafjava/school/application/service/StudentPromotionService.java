package com.altafjava.school.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.lifecycle.LifecycleChange;
import com.altafjava.school.application.lifecycle.LifecycleRecorder;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.event.StudentPromotedEvent;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.PromotionOutcome;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Moves a classroom into the next academic year as one all-or-nothing step. The old classroom link
 * stays in place, so past attendance, grades and report cards keep pointing at the right classroom.
 */
@Service
@RequiredArgsConstructor
public class StudentPromotionService {

	public record Decision(String studentPublicId, PromotionOutcome outcome, String targetClassroomPublicId,
			String reason) {
	}

	public record PreviewRow(Long studentId, String studentPublicId, EnrollmentStatus enrollmentStatus,
			boolean alreadyPlaced, String blockedReason) {
	}

	public record Preview(Long sourceClassroomId, Long targetClassroomId, List<PreviewRow> rows, Integer seatsLeft) {

		public Preview {
			rows = List.copyOf(rows);
		}
	}

	public record ResultRow(Long studentId, String studentPublicId, PromotionOutcome outcome, Long classroomId,
			String classroomPublicId) {
	}

	private final ClassroomRepository classroomRepository;
	private final AcademicYearRepository academicYearRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentRepository studentRepository;
	private final ClassroomService classroomService;
	private final StudentService studentService;
	private final LifecycleRecorder lifecycleRecorder;
	private final EventPublisher eventPublisher;

	/** Changes nothing: shows who can move and why anyone cannot. */
	@Transactional(readOnly = true)
	public Preview preview(String sourceClassroomPublicId, String targetClassroomPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom source = requireClassroom(tenantId, sourceClassroomPublicId);
		Classroom target = requireClassroom(tenantId, targetClassroomPublicId);
		requireLaterYear(tenantId, source, target);
		Map<Long, Student> roster = rosterOf(tenantId, source);
		Set<Long> placed = placedIn(tenantId, roster.keySet(), target.getAcademicYearId());
		List<PreviewRow> rows = roster.values().stream()
				.map(student -> new PreviewRow(student.getId(), student.getPublicId().toString(),
						student.getEnrollmentStatus(), placed.contains(student.getId()),
						blockedReason(student, placed)))
				.toList();
		return new Preview(source.getId(), target.getId(), rows, seatsLeft(tenantId, target));
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Classroom", details = "Students promoted")
	public List<ResultRow> promote(String sourceClassroomPublicId, String defaultTargetClassroomPublicId,
			List<Decision> decisions) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom source = requireClassroom(tenantId, sourceClassroomPublicId);
		Map<Long, Student> roster = rosterOf(tenantId, source);
		Map<String, Student> rosterByPublicId = roster.values().stream()
				.collect(Collectors.toMap(student -> student.getPublicId().toString(), Function.identity()));
		List<ResultRow> results = new ArrayList<>();
		for (Decision decision : decisions) {
			Student student = rosterByPublicId.get(decision.studentPublicId());
			if (student == null) {
				throw new BusinessException(
						"Student " + decision.studentPublicId() + " is not in classroom " + sourceClassroomPublicId);
			}
			results.add(apply(tenantId, source, student, decision, defaultTargetClassroomPublicId));
		}
		return results;
	}

	private ResultRow apply(Long tenantId, Classroom source, Student student, Decision decision,
			String defaultTargetClassroomPublicId) {
		LifecycleChange change = LifecycleChange.of(decision.reason());
		if (!decision.outcome().needsClassroom()) {
			String studentPublicId = student.getPublicId().toString();
			if (decision.outcome() == PromotionOutcome.GRADUATE) {
				studentService.graduate(studentPublicId, change);
			} else {
				studentService.withdraw(studentPublicId, change);
			}
			return new ResultRow(student.getId(), studentPublicId, decision.outcome(), null, null);
		}
		if (student.getEnrollmentStatus() != EnrollmentStatus.ACTIVE) {
			throw new BusinessException("Only an active student can move up, " + student.getPublicId() + " is "
					+ student.getEnrollmentStatus());
		}
		Classroom target = requireClassroom(tenantId, targetFor(decision, defaultTargetClassroomPublicId));
		requireLaterYear(tenantId, source, target);
		seat(tenantId, source, student, target, decision, change);
		return new ResultRow(student.getId(), student.getPublicId().toString(), decision.outcome(), target.getId(),
				target.getPublicId().toString());
	}

	// The shared target is for the class moving up; a repeater goes to a classroom of their own grade.
	private static String targetFor(Decision decision, String defaultTargetClassroomPublicId) {
		if (decision.targetClassroomPublicId() != null) {
			return decision.targetClassroomPublicId();
		}
		if (decision.outcome() == PromotionOutcome.PROMOTE && defaultTargetClassroomPublicId != null) {
			return defaultTargetClassroomPublicId;
		}
		throw new BusinessException("Student " + decision.studentPublicId() + " needs a target classroom");
	}

	private void seat(Long tenantId, Classroom source, Student student, Classroom target, Decision decision,
			LifecycleChange change) {
		AcademicYear targetYear = requireYear(tenantId, target.getAcademicYearId());
		classroomService.enrollStudent(target.getPublicId().toString(), student.getPublicId().toString(),
				targetYear.getPublicId().toString());
		if (decision.outcome() == PromotionOutcome.PROMOTE) {
			lifecycleRecorder.promoted(student.getId(), change);
			eventPublisher.publish(new StudentPromotedEvent(tenantId, student.getId(), source.getId(), target.getId(),
					targetYear.getId()));
		} else {
			lifecycleRecorder.retained(student.getId(), change);
		}
	}

	private Map<Long, Student> rosterOf(Long tenantId, Classroom classroom) {
		List<Long> studentIds = studentClassroomLinkRepository.findAllByClassroomId(tenantId, classroom.getId())
				.stream().map(StudentClassroomLink::getStudentId).toList();
		return studentRepository.findAllByIdInAndTenantId(studentIds, tenantId).stream()
				.collect(Collectors.toMap(Student::getId, Function.identity()));
	}

	private Set<Long> placedIn(Long tenantId, Set<Long> studentIds, Long academicYearId) {
		return studentClassroomLinkRepository.findByStudentIdsAndAcademicYearId(tenantId, studentIds, academicYearId)
				.stream().map(StudentClassroomLink::getStudentId).collect(Collectors.toSet());
	}

	private static String blockedReason(Student student, Set<Long> placed) {
		if (placed.contains(student.getId())) {
			return "Already has a classroom in the target academic year";
		}
		return student.getEnrollmentStatus() == EnrollmentStatus.ACTIVE ? null
				: "Student is " + student.getEnrollmentStatus();
	}

	private Integer seatsLeft(Long tenantId, Classroom target) {
		if (target.getCapacity() == null) {
			return null;
		}
		long taken = studentClassroomLinkRepository.countByClassroomId(tenantId, target.getId());
		return Math.max(target.getCapacity() - Math.toIntExact(taken), 0);
	}

	private void requireLaterYear(Long tenantId, Classroom source, Classroom target) {
		AcademicYear from = requireYear(tenantId, source.getAcademicYearId());
		AcademicYear to = requireYear(tenantId, target.getAcademicYearId());
		if (!to.getStartDate().isAfter(from.getStartDate())) {
			throw new BusinessException("The target classroom must belong to a later academic year than the source");
		}
	}

	private Classroom requireClassroom(Long tenantId, String publicId) {
		return classroomRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + publicId));
	}

	private AcademicYear requireYear(Long tenantId, Long academicYearId) {
		return academicYearRepository.findByIdAndTenantId(academicYearId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearId));
	}
}
