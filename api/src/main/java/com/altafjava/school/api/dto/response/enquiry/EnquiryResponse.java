package com.altafjava.school.api.dto.response.enquiry;

import java.time.Instant;
import java.time.LocalDate;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.enquiry.model.EnquirySource;
import com.altafjava.school.domain.enquiry.model.EnquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A family's admission enquiry and where its follow-up stands.")
public record EnquiryResponse(String publicId, Long version, String childFirstName, String childLastName,
		LocalDate childDateOfBirth, String gradeSought, String guardianFirstName, String guardianLastName,
		String guardianEmail, String guardianPhone, String notes, EnquirySource source, EnquiryStatus status,
		String assignedEmployeePublicId, ReferenceSummary assignedEmployee, LocalDate nextFollowUpOn,
		@NoLabel("The application carries the same family, named on this enquiry") String admissionPublicId,
		Instant closedAt, String closeReason, Instant createdAt) {
}
