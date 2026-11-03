package com.altafjava.school.application.importing;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.domain.common.model.Gender;
import com.altafjava.school.domain.importing.model.ImportType;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

/** A student per row. A blank student code takes the next one from the school's sequence. */
@Component
@RequiredArgsConstructor
class StudentRowImporter implements RowImporter {

	private final StudentService studentService;

	@Override
	public ImportType type() {
		return ImportType.STUDENTS;
	}

	@Override
	public String requiredPermission() {
		return SchoolPermissions.STUDENT_MANAGE;
	}

	@Override
	public List<ImportField> fields() {
		return List.of(ImportField.optional("studentCode", "Admission number; generated when blank"),
				ImportField.required("firstName", "First name"),
				ImportField.required("lastName", "Last name"),
				ImportField.optional("email", "Email address"),
				ImportField.optional("dateOfBirth", "Date of birth, like 2015-04-30"),
				ImportField.optional("gender", "MALE, FEMALE or OTHER"));
	}

	@Override
	public Optional<String> uniqueKey(ImportRow row) {
		return Optional.ofNullable(row.text("studentCode")).map(code -> code.toLowerCase(Locale.ROOT));
	}

	@Override
	public void apply(ImportRow row) {
		studentService.enroll(row.text("studentCode"), row.requiredText("firstName"), row.requiredText("lastName"),
				row.text("email"), row.date("dateOfBirth"), row.choice("gender", Gender.class, Gender.NOT_SPECIFIED),
				null);
	}
}
