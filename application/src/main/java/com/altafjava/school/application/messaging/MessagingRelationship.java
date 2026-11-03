package com.altafjava.school.application.messaging;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.security.TeachingAssignmentResolver;
import com.altafjava.school.application.security.TeachingAssignments;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import lombok.RequiredArgsConstructor;

/**
 * The one rule behind every message: this guardian holds this child, and this teacher teaches them
 * this year. It is checked on each send, so a thread goes quiet when either relationship ends.
 */
@Component
@RequiredArgsConstructor
public class MessagingRelationship {

	private final StudentGuardianLinkRepository studentGuardianLinkRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final AcademicYearRepository academicYearRepository;
	private final TeachingAssignmentResolver teachingAssignmentResolver;

	public void assertAllowed(Long tenantId, Long guardianId, Long teacherUserId, Long studentId) {
		boolean linked = studentGuardianLinkRepository
				.findByGuardianIdAndStudentIdAndTenantId(guardianId, studentId, tenantId)
				.filter(link -> !link.isCustodyRestricted())
				.isPresent();
		if (!linked || !teaches(tenantId, teacherUserId, studentId)) {
			throw new AccessDeniedException("Messages are only between a guardian and a teacher of their child");
		}
	}

	private boolean teaches(Long tenantId, Long teacherUserId, Long studentId) {
		TeachingAssignments teaching = teachingAssignmentResolver.forUser(teacherUserId, tenantId);
		return academicYearRepository.findFirstByCurrentTrueAndTenantIdOrderByStartDateDesc(tenantId)
				.flatMap(year -> studentClassroomLinkRepository.findByStudentIdAndAcademicYearId(tenantId, studentId,
						year.getId()))
				.map(StudentClassroomLink::getClassroomId)
				.filter(teaching::teachesClassroom)
				.isPresent();
	}
}
