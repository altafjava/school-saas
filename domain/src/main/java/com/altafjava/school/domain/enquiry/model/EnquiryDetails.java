package com.altafjava.school.domain.enquiry.model;

import java.time.LocalDate;

/** Who is asking and for whom. The child's date of birth is optional until the family applies. */
public record EnquiryDetails(String childFirstName, String childLastName, LocalDate childDateOfBirth,
		String gradeSought, String guardianFirstName, String guardianLastName, String guardianEmail,
		String guardianPhone) {
}
