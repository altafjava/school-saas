package com.altafjava.school.application.policy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.security.ResourceAccessPolicy;
import com.altafjava.school.application.security.OwnStudentResolver;
import com.altafjava.school.application.security.TeachingAssignmentResolver;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

// School-specific rules applied after platform RBAC passes; other resource types default to allowed.
@Component
@RequiredArgsConstructor
public class SchoolResourceAccessPolicy implements ResourceAccessPolicy {

	private final ClassroomRepository classroomRepository;
	private final StudentRepository studentRepository;
	private final TeachingAssignmentResolver teachingAssignmentResolver;
	private final OwnStudentResolver ownStudentResolver;

	@Override
	public boolean isAllowed(String userId, Long tenantId, String resourceType, String resourceId, String action) {
		if (!ResourceAction.READ.name().equals(action)) {
			return true;
		}
		if (ResourceType.CLASSROOM.name().equals(resourceType)) {
			return teachesClassroom(userId, resourceId, tenantId);
		}
		if (ResourceType.STUDENT.name().equals(resourceType)) {
			return isOwnStudent(userId, resourceId, tenantId);
		}
		return true;
	}

	private boolean teachesClassroom(String userId, String classroomPublicId, Long tenantId) {
		try {
			return classroomRepository.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
					.map(Classroom::getId)
					.map(teachingAssignmentResolver.forUser(Long.valueOf(userId), tenantId)::teachesClassroom)
					.orElse(false);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private boolean isOwnStudent(String userId, String studentPublicId, Long tenantId) {
		try {
			return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
					.map(Student::getId)
					.map(ownStudentResolver.forUser(Long.valueOf(userId), tenantId)::contains)
					.orElse(false);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
