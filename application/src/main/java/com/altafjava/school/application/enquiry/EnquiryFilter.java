package com.altafjava.school.application.enquiry;

import java.time.LocalDate;
import com.altafjava.school.domain.enquiry.model.EnquirySource;
import com.altafjava.school.domain.enquiry.model.EnquiryStatus;

/** Narrows the enquiry list; every part is optional. {@code q} matches the child's or guardian's name. */
public record EnquiryFilter(EnquiryStatus status, EnquirySource source, String assignedEmployeePublicId,
		LocalDate followUpDueBy, String q) {
}
