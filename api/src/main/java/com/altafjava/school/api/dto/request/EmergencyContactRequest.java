package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmergencyContactRequest(
		@NotBlank @Size(max = 200) String name,
		@NotBlank @Size(max = 100) String relationship,
		@NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{7,30}$", message = "must be a valid phone number") String phone,
		@Pattern(regexp = "^\\+?[0-9 ()-]{7,30}$", message = "must be a valid phone number") String alternatePhone,
		@Min(1) int priority) {
}
