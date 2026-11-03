package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.enquiry.model.EnquirySource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A new enquiry. A phone number or an email address is required to follow it up.")
public record ReceiveEnquiryRequest(
		@NotBlank @Size(max = 100) String childFirstName,
		@NotBlank @Size(max = 100) String childLastName,
		@Past LocalDate childDateOfBirth,
		@NotBlank @Size(max = 20) String gradeSought,
		@NotBlank @Size(max = 100) String guardianFirstName,
		@NotBlank @Size(max = 100) String guardianLastName,
		@Email @Size(max = 255) String guardianEmail,
		@Size(max = 30) String guardianPhone,
		@NotNull EnquirySource source,
		@Size(max = 2000) String notes,
		String assignedEmployeePublicId) {
}
