package com.altafjava.school.application.policy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.security.ResourceAccessPolicy;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;

/**
 * School-specific authorization rules applied after platform RBAC passes.
 *
 * Rule: a teacher may only READ their own classroom — not other classrooms in the same tenant.
 * All other resource types default to allowed (RBAC is the primary gate).
 */
@Component
public class SchoolResourceAccessPolicy implements ResourceAccessPolicy {

    private final ClassroomRepository classroomRepository;

    public SchoolResourceAccessPolicy(ClassroomRepository classroomRepository) {
        this.classroomRepository = classroomRepository;
    }

    @Override
    public boolean isAllowed(String userId, Long tenantId, String resourceType, String resourceId, String action) {
        if ("CLASSROOM".equals(resourceType) && "READ".equals(action)) {
            return isTeacherAssignedToClassroom(userId, resourceId, tenantId);
        }
        return true;
    }

    private boolean isTeacherAssignedToClassroom(String userId, String classroomPublicId, Long tenantId) {
        try {
            UUID classroomUuid = UUID.fromString(classroomPublicId);
            return classroomRepository.findByPublicIdAndTenantId(classroomUuid, tenantId)
                    .map(classroom -> userId.equals(String.valueOf(classroom.getClassTeacherId())))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
