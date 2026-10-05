package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record StudentResponse(
		String publicId,
		String studentCode,
		String firstName,
		String lastName,
		String email,
		String phone,
		LocalDate dateOfBirth,
		String enrollmentStatus,
		AddressResponse address,
		String photoFilePublicId) {
}
