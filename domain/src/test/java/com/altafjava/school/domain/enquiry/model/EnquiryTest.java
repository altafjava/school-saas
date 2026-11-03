package com.altafjava.school.domain.enquiry.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class EnquiryTest {

	@Test
	void receive_withoutAnyWayToReachTheFamily_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> Enquiry.receive(
				new EnquiryDetails("Noa", "Lee", null, "3", "Ola", "Lee", null, null), EnquirySource.WALK_IN, null));
	}

	@Test
	void firstFollowUp_takesANewEnquiryIntoProgress() {
		Enquiry enquiry = newEnquiry();
		LocalDate next = LocalDate.of(2026, 11, 9);

		enquiry.recordFollowUp(next);

		assertEquals(EnquiryStatus.IN_PROGRESS, enquiry.getStatus());
		assertEquals(next, enquiry.getNextFollowUpOn());
	}

	@Test
	void close_thenReopen_returnsToProgress() {
		Enquiry enquiry = newEnquiry();
		enquiry.close("No seats in grade 3");

		assertThrows(BusinessException.class, () -> enquiry.recordFollowUp(null));
		enquiry.reopen();
		assertEquals(EnquiryStatus.IN_PROGRESS, enquiry.getStatus());
		assertNull(enquiry.getCloseReason());
	}

	@Test
	void converted_isFinal() {
		Enquiry enquiry = newEnquiry();
		enquiry.convertedInto(9L);

		assertEquals(9L, enquiry.getAdmissionId());
		assertThrows(BusinessException.class, () -> enquiry.close("late"));
		assertThrows(BusinessException.class, () -> enquiry.convertedInto(10L));
		assertThrows(BusinessException.class, () -> enquiry.assignTo(3L));
	}

	@Test
	void closedEnquiry_cannotBeConvertedWithoutReopening() {
		Enquiry enquiry = newEnquiry();
		enquiry.close("No answer");

		assertThrows(BusinessException.class, () -> enquiry.convertedInto(9L));
	}

	private static Enquiry newEnquiry() {
		return Enquiry.receive(new EnquiryDetails("Noa", "Lee", null, "3", "Ola", "Lee", "ola@x.test", null),
				EnquirySource.WEBSITE, null);
	}
}
