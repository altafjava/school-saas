package com.altafjava.school.api.dto.response;

import com.altafjava.school.api.support.NoLabel;

public record RollNumberResponse(
		@NoLabel("Reply to a single write whose caller just sent this id") String studentPublicId,
		@NoLabel("Reply to a single write whose caller just sent this id") String classroomPublicId,
		String rollNumber) {
}
