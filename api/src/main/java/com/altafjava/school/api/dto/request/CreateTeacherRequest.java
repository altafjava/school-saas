package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.common.model.Gender;

// employeeCode is an explicit-override path — omit it (null/blank) to have the tenant's
// configured numbering sequence generate one; see TeacherService#hire.
public record CreateTeacherRequest(
		@Size(max = 50) String employeeCode,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Email @Size(max = 255) String email,
		Gender gender,
		@NotNull LocalDate joinDate) {
}
