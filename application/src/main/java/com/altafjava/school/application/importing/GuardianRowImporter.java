package com.altafjava.school.application.importing;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.importing.model.ImportType;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * A guardian and one of their children per row. A guardian already on file with the same email is
 * linked rather than created again, so a family with two children takes two rows.
 */
@Component
@RequiredArgsConstructor
class GuardianRowImporter implements RowImporter {

	private final GuardianService guardianService;
	private final GuardianRepository guardianRepository;
	private final StudentRepository studentRepository;

	@Override
	public ImportType type() {
		return ImportType.GUARDIANS;
	}

	@Override
	public String requiredPermission() {
		return SchoolPermissions.GUARDIAN_MANAGE;
	}

	@Override
	public List<ImportField> fields() {
		return List.of(ImportField.required("studentCode", "The child's admission number"),
				ImportField.required("firstName", "Guardian's first name"),
				ImportField.required("lastName", "Guardian's last name"),
				ImportField.required("phone", "Phone number, like +919876543210"),
				ImportField.optional("email", "Email address; links to a guardian already on file"),
				ImportField.required("relationship", "MOTHER, FATHER, LEGAL_GUARDIAN or OTHER"),
				ImportField.optional("primaryContact", "yes or no"));
	}

	@Override
	public Optional<String> uniqueKey(ImportRow row) {
		String contact = Optional.ofNullable(row.text("email")).orElse(row.text("phone"));
		return contact == null || row.text("studentCode") == null ? Optional.empty()
				: Optional.of((row.text("studentCode") + "|" + contact).toLowerCase(Locale.ROOT));
	}

	@Override
	public void apply(ImportRow row) {
		Long tenantId = TenantContext.getCurrentTenantId();
		String studentCode = row.requiredText("studentCode");
		Student student = studentRepository.findByStudentCodeAndTenantId(studentCode, tenantId)
				.orElseThrow(() -> new ImportRowException("studentCode", "No student has the code " + studentCode));
		RelationshipType relationship = row.requiredChoice("relationship", RelationshipType.class);
		boolean primary = row.flag("primaryContact");
		String email = row.text("email");
		Guardian guardian = (email == null ? Optional.<Guardian>empty()
				: guardianRepository.findFirstByEmailAndTenantIdOrderByIdAsc(email, tenantId))
				.orElseGet(() -> guardianService.create(row.requiredText("firstName"), row.requiredText("lastName"),
						email, row.requiredText("phone"), null));
		guardianService.linkToStudent(guardian.getPublicId().toString(), student.getPublicId().toString(),
				relationship, primary);
	}
}
