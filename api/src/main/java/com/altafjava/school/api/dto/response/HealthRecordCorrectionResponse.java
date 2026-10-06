package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record HealthRecordCorrectionResponse(
		String publicId,
		String oldBloodGroup,
		String oldAllergies,
		String oldConditions,
		String oldImmunizations,
		String newBloodGroup,
		String newAllergies,
		String newConditions,
		String newImmunizations,
		String correctedBy,
		Instant correctedAt) {
}
