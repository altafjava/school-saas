package com.altafjava.school.application.home;

import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.subject.model.Subject;

/** Labels for what a home card points at. */
final class HomeReferences {

	private HomeReferences() {
	}

	static ReferenceSummary of(Classroom classroom) {
		return new ReferenceSummary(classroom.getPublicId().toString(), classroom.displayLabel(),
				classroom.getClassCode());
	}

	static ReferenceSummary of(Subject subject) {
		return new ReferenceSummary(subject.getPublicId().toString(), subject.getName(), subject.getCode());
	}

	static ReferenceSummary of(Employee employee) {
		return new ReferenceSummary(employee.getPublicId().toString(),
				employee.getFirstName() + " " + employee.getLastName(), employee.getEmployeeCode());
	}

	static ReferenceSummary of(Student student) {
		return new ReferenceSummary(student.getPublicId().toString(),
				student.getFirstName() + " " + student.getLastName(), student.getStudentCode());
	}
}
