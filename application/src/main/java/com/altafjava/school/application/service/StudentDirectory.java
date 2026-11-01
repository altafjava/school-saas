package com.altafjava.school.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.filter.StudentFilter;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.student.model.Student;
import lombok.RequiredArgsConstructor;

/** Student reads as a caller sees them: limited to their reach unless they hold STUDENT_READ_ALL. */
@Service
@RequiredArgsConstructor
public class StudentDirectory {

	private final StudentService studentService;
	private final AcademicAccessGuard academicAccessGuard;
	private final StudentDataAccessGuard studentDataAccessGuard;

	public Page<Student> search(StudentFilter filter, Pageable pageable) {
		return academicAccessGuard.studentListReach(TenantContext.getCurrentTenantId())
				.map(classroomIds -> studentService.searchStudentsWithin(filter, classroomIds, pageable))
				.orElseGet(() -> studentService.searchStudents(filter, pageable));
	}

	public Student get(String publicId) {
		studentDataAccessGuard.assertCanView(TenantContext.getCurrentTenantId(), publicId);
		return studentService.findByPublicId(publicId);
	}
}
