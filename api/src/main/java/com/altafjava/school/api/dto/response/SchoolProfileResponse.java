package com.altafjava.school.api.dto.response;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Which school records the signed-in user is. Each id is absent when the user has no such record.")
public record SchoolProfileResponse(
		@Schema(description = "The user's own student record") String studentId,
		@Schema(description = "The user's staff record (teachers are staff too)") String employeeId,
		@Schema(description = "True when the user is also a teacher") boolean teacher,
		@Schema(description = "The user's guardian record") String guardianId,
		@Schema(description = "Students linked to the guardian record; empty for a non-guardian") List<LinkedStudent> linkedStudents) {

	public SchoolProfileResponse {
		linkedStudents = List.copyOf(linkedStudents);
	}

	public record LinkedStudent(String id, String name, String studentCode) {
	}
}
