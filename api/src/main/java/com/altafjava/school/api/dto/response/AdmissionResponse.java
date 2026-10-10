package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.admission.model.ApplicationFeeStatus;
import com.altafjava.school.domain.common.model.Gender;

public record AdmissionResponse(
		String publicId,
		Long version,
		String applicantFirstName,
		String applicantLastName,
		LocalDate applicantDateOfBirth,
		Gender applicantGender,
		String guardianFirstName,
		String guardianLastName,
		String guardianEmail,
		String guardianPhone,
		String appliedGrade,
		AdmissionStatus status,
		Instant submittedAt,
		BigDecimal entranceTestScore,
		BigDecimal entranceTestMaxScore,
		Integer meritRank,
		ApplicationFeeStatus applicationFeeStatus,
		BigDecimal applicationFeeAmount,
		String applicationFeeReceiptNumber,
		Instant applicationFeePaidAt,
		boolean hasOfferLetter) {
}
