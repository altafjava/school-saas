package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BulkRecordGradesRequest(
		@NotBlank String examPublicId,
		@NotEmpty @Size(max = 500) List<@Valid Row> rows) {

	public BulkRecordGradesRequest {
		rows = rows == null ? null : List.copyOf(rows);
	}

	public record Row(@NotBlank String studentPublicId, @NotNull @DecimalMin("0.0") BigDecimal marks) {
	}
}
