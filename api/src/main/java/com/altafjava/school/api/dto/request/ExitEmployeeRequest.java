package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ExitEmployeeRequest(
		@Schema(description = "RESIGNED, TERMINATED or RETIRED") @NotNull EmployeeStatus status,
		@NotNull @PastOrPresent LocalDate exitDate,
		@Size(max = 500) String reason) {
}
