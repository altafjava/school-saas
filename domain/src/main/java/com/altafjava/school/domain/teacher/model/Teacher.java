package com.altafjava.school.domain.teacher.model;

import java.time.LocalDate;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The teaching role of an {@link Employee}: same id and public id, plus the guarantee that
 * classrooms, timetable entries, lessons and assignments can only reference teaching staff. All
 * HR data lives on {@code Employee}; this type adds none of its own yet.
 */
@Entity
@Table(name = "teachers")
@PrimaryKeyJoinColumn(name = "id")
@SuperBuilder
@NoArgsConstructor
public class Teacher extends Employee {

	public static Teacher create(String employeeCode, String firstName, String lastName, String email,
			LocalDate joinDate) {
		return Teacher.builder()
				.staffCategory(StaffCategory.TEACHING)
				.status(EmployeeStatus.ACTIVE)
				.employeeCode(employeeCode)
				.firstName(firstName)
				.lastName(lastName)
				.email(email)
				.joinDate(joinDate)
				.build();
	}
}
