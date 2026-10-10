package com.altafjava.school.application.security;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.TimetableSubstitutionRepository;
import lombok.RequiredArgsConstructor;

/**
 * Enforces the caller's {@link AcademicScope} on single classroom-scoped records. Controllers
 * check the permission; services call this to check that the permission reaches the record.
 */
@Component
@RequiredArgsConstructor
public class AcademicAccessGuard {

	private static final String STUDENT_READ = "STUDENT_READ";

	private final AcademicScopeResolver academicScopeResolver;
	private final PermissionAuthorizationService permissionAuthorizationService;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final TimetableSubstitutionRepository timetableSubstitutionRepository;

	/** A record about one student of a classroom, such as an attendance entry. */
	public void assertCanReadStudentRecord(Long tenantId, Long classroomId, Long studentId) {
		AcademicScope scope = academicScopeResolver.current(tenantId);
		if (scope.canReadClassroom(classroomId) || scope.ownsStudent(studentId) || mayViewAnyStudent()) {
			return;
		}
		throw new AccessDeniedException("Not authorized to view this student's record");
	}

	/** The roster exposes every classmate's personal details, so being enrolled is not enough. */
	public void assertCanReadRoster(Long tenantId, Long classroomId) {
		if (academicScopeResolver.current(tenantId).canReadClassroom(classroomId) || mayViewAnyStudent()) {
			return;
		}
		throw new AccessDeniedException("Not authorized to view this classroom's roster");
	}

	/** Lessons and assignments: the staff who teach the classroom, and the students enrolled in it. */
	public void assertCanViewCoursework(Long tenantId, Long classroomId) {
		AcademicScope scope = academicScopeResolver.current(tenantId);
		if (scope.canReadClassroom(classroomId) || hasOwnStudentEnrolled(scope, tenantId, classroomId)) {
			return;
		}
		throw new AccessDeniedException("Not authorized to view this classroom's coursework");
	}

	/**
	 * The classrooms a coursework list may draw from. For a requested classroom the caller must be able to view
	 * it, and it alone is reachable; with none, they are the ones the caller teaches plus those their own
	 * students attend.
	 */
	public CourseworkReach courseworkReach(Long tenantId, Long requestedClassroomId) {
		if (requestedClassroomId != null) {
			assertCanViewCoursework(tenantId, requestedClassroomId);
			return CourseworkReach.ALL;
		}
		AcademicScope scope = academicScopeResolver.current(tenantId);
		if (scope.readsAllClassrooms()) {
			return CourseworkReach.ALL;
		}
		Set<Long> classroomIds = new HashSet<>(scope.teaching().classroomIds());
		if (!scope.ownStudentIds().isEmpty()) {
			studentClassroomLinkRepository
					.findByStudentIdIn(tenantId, List.copyOf(scope.ownStudentIds()))
					.forEach(link -> classroomIds.add(link.getClassroomId()));
		}
		return new CourseworkReach(false, classroomIds);
	}

	public void assertCanWriteClassroom(Long tenantId, Long classroomId) {
		if (!academicScopeResolver.current(tenantId).canWriteClassroom(classroomId)) {
			throw new AccessDeniedException("Not authorized to record for this classroom");
		}
	}

	public void assertCanReadSubject(Long tenantId, Long classroomId, Long subjectId) {
		if (!academicScopeResolver.current(tenantId).canReadSubject(classroomId, subjectId)) {
			throw new AccessDeniedException("Not authorized to view this subject's records in this classroom");
		}
	}

	public void assertCanWriteSubject(Long tenantId, Long classroomId, Long subjectId) {
		if (!academicScopeResolver.current(tenantId).canWriteSubject(classroomId, subjectId)) {
			throw new AccessDeniedException("Not authorized to record for this subject in this classroom");
		}
	}

	/** For coursework that is attributed to its teacher: returns the caller's teacher id. */
	public Long requireTeacherOfSubject(Long tenantId, Long classroomId, Long subjectId) {
		AcademicScope scope = academicScopeResolver.current(tenantId);
		if (!scope.teaching().isTeacher()) {
			throw new AccessDeniedException("No teacher record linked to the current user");
		}
		if (!scope.canWriteSubject(classroomId, subjectId)) {
			throw new AccessDeniedException("Not authorized to record for this subject in this classroom");
		}
		return scope.teaching().teacherId();
	}

	/** A period belongs to its timetabled teacher, or to whoever substitutes for them that day. */
	public void assertCanMarkPeriod(Long tenantId, TimetableEntry entry, LocalDate date) {
		AcademicScope scope = academicScopeResolver.current(tenantId);
		if (scope.canWriteSubject(entry.getClassroomId(), entry.getSubjectId())) {
			return;
		}
		Long teacherId = scope.teaching().teacherId();
		if (teacherId != null && timetableSubstitutionRepository.existsActiveForSubstitute(tenantId, entry.getId(),
				date, teacherId)) {
			return;
		}
		throw new AccessDeniedException("Not authorized to mark attendance for this period");
	}

	private boolean hasOwnStudentEnrolled(AcademicScope scope, Long tenantId, Long classroomId) {
		return !scope.ownStudentIds().isEmpty() && studentClassroomLinkRepository
				.existsByClassroomIdAndStudentIdIn(tenantId, classroomId, scope.ownStudentIds());
	}

	private boolean mayViewAnyStudent() {
		return permissionAuthorizationService.hasPermission(STUDENT_READ);
	}
}
