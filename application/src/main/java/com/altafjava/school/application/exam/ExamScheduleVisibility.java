package com.altafjava.school.application.exam;

import java.util.Set;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

/**
 * Families see an exam once its classroom's schedule or its results are published; staff who plan
 * exams, or who teach the classroom, see it from the moment it is scheduled.
 */
@Component
@RequiredArgsConstructor
public class ExamScheduleVisibility {

	private final AcademicScopeResolver academicScopeResolver;
	private final PermissionAuthorizationService permissionAuthorizationService;

	/** {@code everySchedule} lets the caller see every unpublished exam; otherwise only those classrooms'. */
	public record Reach(boolean everySchedule, Set<Long> staffClassroomIds) {

		public Reach {
			staffClassroomIds = Set.copyOf(staffClassroomIds);
		}

		public boolean sees(Exam exam) {
			return everySchedule || exam.isSchedulePublished() || exam.isResultsPublished()
					|| staffClassroomIds.contains(exam.getClassroomId());
		}
	}

	public Reach current(Long tenantId) {
		AcademicScope scope = academicScopeResolver.current(tenantId);
		boolean every = scope.readsAllClassrooms()
				|| permissionAuthorizationService.hasPermission(SchoolPermissions.EXAM_WRITE);
		return new Reach(every, every ? Set.of() : scope.teaching().classroomIds());
	}
}
