package com.altafjava.school.demo;

import java.time.LocalDate;
import java.util.List;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.teacher.model.Teacher;

/** What the core seed created, handed to the per-module seeders that build on it. */
record DemoSchool(Long tenantId, Long adminUserId, LocalDate today, AcademicYear year, List<Teacher> teachers,
		List<Employee> staff, List<Student> students, List<Guardian> guardians) {

	/** The teacher, student and parent tied to the demo logins. */
	Teacher loginTeacher() {
		return teachers.get(0);
	}

	Student loginStudent() {
		return students.get(0);
	}

	Guardian loginParent() {
		return guardians.get(0);
	}
}
