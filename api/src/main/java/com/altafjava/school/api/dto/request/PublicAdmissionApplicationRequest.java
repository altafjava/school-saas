package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.common.model.Gender;

/**
 * Same field set as {@link SubmitAdmissionRequest} — deliberately a separate type (not reused)
 * because this one is bound to the unauthenticated {@code POST /api/v1/admissions/apply}
 * endpoint: it must never grow a {@code status} or saga-tracking field an anonymous caller could
 * set, even if the admin-facing request DTO later does.
 */
public record PublicAdmissionApplicationRequest(
		@NotBlank @Size(max = 100) String applicantFirstName,
		@NotBlank @Size(max = 100) String applicantLastName,
		LocalDate applicantDateOfBirth,
		Gender applicantGender,
		@NotBlank @Size(max = 100) String guardianFirstName,
		@NotBlank @Size(max = 100) String guardianLastName,
		@Email @Size(max = 255) String guardianEmail,
		@Size(max = 30) String guardianPhone,
		@NotBlank @Size(max = 20) String appliedGrade) {
}
