package com.altafjava.school.application.security;

import org.springframework.stereotype.Component;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import lombok.RequiredArgsConstructor;

/**
 * Decides who may see grades of an exam whose results are not yet published: the staff who enter
 * or publish them. Students and guardians only ever see published results.
 */
@Component
@RequiredArgsConstructor
public class ExamResultVisibilityPolicy {

	private final PermissionAuthorizationService permissionAuthorizationService;

	public boolean canSeeUnpublishedResults() {
		return permissionAuthorizationService.hasPermission("STUDENT_GRADES_WRITE")
				|| permissionAuthorizationService.hasPermission("EXAM_RESULT_PUBLISH");
	}
}
