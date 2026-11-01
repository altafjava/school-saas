package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.api.support.NoLabel;

public record StudentClassroomLinkResponse(
		String publicId,
		@NoLabel("Reply to a single write whose caller just sent this id") String studentPublicId,
		@NoLabel("Reply to a single write whose caller just sent this id") String classroomPublicId,
		@NoLabel("Reply to a single write whose caller just sent this id") String academicYearPublicId,
		LocalDate enrolledAt,
		String rollNumber) {
}
