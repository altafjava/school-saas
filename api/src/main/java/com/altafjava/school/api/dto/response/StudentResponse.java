package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.domain.common.model.Gender;

public record StudentResponse(
		String publicId,
		Long version,
		String studentCode,
		String firstName,
		String lastName,
		String email,
		String phone,
		LocalDate dateOfBirth,
		Gender gender,
		String enrollmentStatus,
		AddressResponse address,
		String photoFilePublicId,
		CurrentClassroomResponse currentClassroom) {

	/** The same student, placed in {@code classroom} (null for a student with no place this academic year). */
	public static StudentResponse placedIn(StudentResponse student, CurrentClassroomResponse classroom) {
		return new StudentResponse(student.publicId, student.version, student.studentCode, student.firstName,
				student.lastName, student.email, student.phone, student.dateOfBirth, student.gender,
				student.enrollmentStatus, student.address, student.photoFilePublicId, classroom);
	}
}
