package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Assign to one student or to a whole classroom; send exactly one of the two")
public record AssignFeeStructureRequest(String studentPublicId, String classroomPublicId) {

	@Schema(hidden = true)
	@AssertTrue(message = "Send either studentPublicId or classroomPublicId, not both")
	public boolean isSingleTarget() {
		return (studentPublicId != null) != (classroomPublicId != null);
	}
}
