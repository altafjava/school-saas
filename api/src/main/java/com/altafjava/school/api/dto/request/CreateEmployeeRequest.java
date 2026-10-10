package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.common.model.Gender;
import com.altafjava.school.domain.employee.model.StaffCategory;
import io.swagger.v3.oas.annotations.media.Schema;

// Non-teaching staff only — teachers are hired through POST /teachers. employeeCode is an
// explicit-override path: omit it to have the tenant's numbering sequence generate one.
public record CreateEmployeeRequest(
		@Schema(description = "ADMINISTRATIVE, SUPPORT or OTHER; teaching staff are hired as teachers") @NotNull StaffCategory staffCategory,
		@Size(max = 50) String employeeCode,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Email @Size(max = 255) String email,
		Gender gender,
		@NotNull LocalDate joinDate) {
}
