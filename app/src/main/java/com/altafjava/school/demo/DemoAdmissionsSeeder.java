package com.altafjava.school.demo;

import java.math.BigDecimal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.enquiry.EnquiryService;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.common.model.Gender;
import com.altafjava.school.domain.enquiry.model.Enquiry;
import com.altafjava.school.domain.enquiry.model.EnquiryDetails;
import com.altafjava.school.domain.enquiry.model.EnquirySource;
import com.altafjava.school.domain.enquiry.model.FollowUpChannel;
import lombok.RequiredArgsConstructor;

/** Enquiries at different stages and applications waiting for review, for the admissions desk. */
@Component
@Profile({ "dev", "test" })
@RequiredArgsConstructor
class DemoAdmissionsSeeder {

	private final EnquiryService enquiryService;
	private final AdmissionService admissionService;

	void seed(DemoSchool school) {
		String clerk = school.staff().get(0).getPublicId().toString();
		Enquiry walkIn = enquiryService.receive(new EnquiryDetails("Rohan", "Malhotra",
				school.today().minusYears(6), "Grade 1", "Neha", "Malhotra", "neha.malhotra@example.com",
				"+919812300001"), EnquirySource.WALK_IN, "Visited with both parents.", clerk);
		enquiryService.recordFollowUp(walkIn.getPublicId().toString(), FollowUpChannel.CALL,
				"Shared the fee structure; they will visit again.", school.today().plusDays(3), school.adminUserId());
		enquiryService.receive(new EnquiryDetails("Tara", "Kapoor", school.today().minusYears(7), "Grade 2",
				"Vikram", "Kapoor", "vikram.kapoor@example.com", null), EnquirySource.WEBSITE, null, clerk);
		enquiryService.receive(new EnquiryDetails("Aryan", "Bhat", null, "Grade 3", "Pooja", "Bhat", null,
				"+919812300003"), EnquirySource.REFERRAL, "Referred by a current parent.", null);

		admissionService.submit("Zara", "Qureshi", school.today().minusYears(6), Gender.FEMALE, "Imran", "Qureshi",
				"imran.qureshi@example.com", "+919812300011", "Grade 1");
		Admission reviewed = admissionService.submit("Dev", "Arora", school.today().minusYears(7), Gender.MALE,
				"Kavita", "Arora", "kavita.arora@example.com", "+919812300012", "Grade 2");
		admissionService.markUnderReview(reviewed.getPublicId().toString());
		admissionService.recordEntranceTestScore(reviewed.getPublicId().toString(), new BigDecimal("42"),
				new BigDecimal("50"));
	}
}
