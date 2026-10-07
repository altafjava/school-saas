package com.altafjava.school.application.policy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.security.ResourceAccessPolicy;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

// School-specific rules applied after platform RBAC passes; other resource types default to allowed.
// A custody-restricted guardian stays linked but loses own-child data access.
@Component
public class SchoolResourceAccessPolicy implements ResourceAccessPolicy {

	private final ClassroomRepository classroomRepository;
	private final StudentRepository studentRepository;
	private final GuardianRepository guardianRepository;
	private final StudentGuardianLinkRepository studentGuardianLinkRepository;
	private final TeacherRepository teacherRepository;

	public SchoolResourceAccessPolicy(ClassroomRepository classroomRepository, StudentRepository studentRepository,
			GuardianRepository guardianRepository, StudentGuardianLinkRepository studentGuardianLinkRepository,
			TeacherRepository teacherRepository) {
		this.classroomRepository = classroomRepository;
		this.studentRepository = studentRepository;
		this.guardianRepository = guardianRepository;
		this.studentGuardianLinkRepository = studentGuardianLinkRepository;
		this.teacherRepository = teacherRepository;
	}

	@Override
	public boolean isAllowed(String userId, Long tenantId, String resourceType, String resourceId, String action) {
		if (ResourceType.CLASSROOM.name().equals(resourceType) && ResourceAction.READ.name().equals(action)) {
			return isTeacherAssignedToClassroom(userId, resourceId, tenantId);
		}
		if (ResourceType.STUDENT.name().equals(resourceType) && ResourceAction.READ.name().equals(action)) {
			return isSelfOrGuardianOfStudent(userId, resourceId, tenantId);
		}
		return true;
	}

	private boolean isTeacherAssignedToClassroom(String userId, String classroomPublicId, Long tenantId) {
		try {
			UUID classroomUuid = UUID.fromString(classroomPublicId);
			Long actingUserId = Long.valueOf(userId);
			// classroom.classTeacherId is a Teacher.id, not a platform users.id — resolve the
			// caller's own Teacher record first, then compare Teacher.id to Teacher.id.
			return teacherRepository.findByUserIdAndTenantId(actingUserId, tenantId)
					.flatMap(teacher -> classroomRepository.findByPublicIdAndTenantId(classroomUuid, tenantId)
							.map(classroom -> teacher.getId().equals(classroom.getClassTeacherId())))
					.orElse(false);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private boolean isSelfOrGuardianOfStudent(String userId, String studentPublicId, Long tenantId) {
		try {
			UUID studentUuid = UUID.fromString(studentPublicId);
			Long actingUserId = Long.valueOf(userId);
			Student student = studentRepository.findByPublicIdAndTenantId(studentUuid, tenantId).orElse(null);
			if (student == null) {
				return false;
			}
			if (actingUserId.equals(student.getUserId())) {
				return true;
			}
			return guardianRepository.findByUserIdAndTenantId(actingUserId, tenantId)
					.flatMap(guardian -> studentGuardianLinkRepository.findByGuardianIdAndStudentIdAndTenantId(
							guardian.getId(), student.getId(), tenantId))
					.map(link -> !link.isCustodyRestricted())
					.orElse(false);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
