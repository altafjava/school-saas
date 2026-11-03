package com.altafjava.school.application.importing;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.service.EmployeeService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.domain.common.model.Gender;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.importing.model.ImportType;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

/** A staff member per row; teaching staff become teachers, as when hired one by one. */
@Component
@RequiredArgsConstructor
class StaffRowImporter implements RowImporter {

	private final EmployeeService employeeService;
	private final TeacherService teacherService;

	@Override
	public ImportType type() {
		return ImportType.STAFF;
	}

	@Override
	public String requiredPermission() {
		return SchoolPermissions.EMPLOYEE_MANAGE;
	}

	@Override
	public List<ImportField> fields() {
		return List.of(ImportField.optional("employeeCode", "Employee code; generated when blank"),
				ImportField.required("firstName", "First name"),
				ImportField.required("lastName", "Last name"),
				ImportField.required("email", "Work email address"),
				ImportField.required("staffCategory", "TEACHING, ADMINISTRATIVE, SUPPORT or OTHER"),
				ImportField.required("joinDate", "Date joined, like 2024-06-01"),
				ImportField.optional("gender", "MALE, FEMALE or OTHER"));
	}

	@Override
	public Optional<String> uniqueKey(ImportRow row) {
		return Optional.ofNullable(row.text("employeeCode")).or(() -> Optional.ofNullable(row.text("email")))
				.map(key -> key.toLowerCase(Locale.ROOT));
	}

	@Override
	public void apply(ImportRow row) {
		StaffCategory category = row.requiredChoice("staffCategory", StaffCategory.class);
		String code = row.text("employeeCode");
		String firstName = row.requiredText("firstName");
		String lastName = row.requiredText("lastName");
		String email = row.requiredText("email");
		Gender gender = row.choice("gender", Gender.class, Gender.NOT_SPECIFIED);
		LocalDate joinDate = row.requiredDate("joinDate");
		if (category == StaffCategory.TEACHING) {
			teacherService.hire(code, firstName, lastName, email, gender, joinDate);
		} else {
			employeeService.hire(category, code, firstName, lastName, email, gender, joinDate);
		}
	}
}
