package com.altafjava.school.application.admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.domain.admission.model.Admission;

@ExtendWith(MockitoExtension.class)
class OfferLetterIssuerTest {

	@Mock
	private DocumentIssuanceService documentIssuanceService;

	private Admission approvedAdmission() {
		Admission admission = Admission.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith", null, null,
				"Grade 3");
		admission.setId(4L);
		admission.setPublicId(UUID.fromString("abcdef12-0000-0000-0000-000000000000"));
		admission.approve();
		return admission;
	}

	private DocumentIssuance issuance(long id) {
		DocumentIssuance issuance = DocumentIssuance.create("ADMISSION_OFFER_LETTER", "ADMISSION", 4L, "Offer",
				"Alice Smith", null, null, "code" + id, "key", null);
		issuance.setId(id);
		return issuance;
	}

	@Test
	void issue_ownsTheLetterByTheAdmissionAndNamesTheApplicant() {
		when(documentIssuanceService.issue(any())).thenReturn(issuance(9L));

		new OfferLetterIssuer(documentIssuanceService).issue(1L, approvedAdmission(), 7L);

		ArgumentCaptor<DocumentIssueRequest> captor = ArgumentCaptor.forClass(DocumentIssueRequest.class);
		verify(documentIssuanceService).issue(captor.capture());
		DocumentIssueRequest request = captor.getValue();
		assertEquals("ADMISSION_OFFER_LETTER", request.documentType());
		assertEquals("ADMISSION", request.ownerEntityType());
		assertEquals(4L, request.ownerEntityId());
		assertEquals("Alice Smith", request.subjectDisplayName());
		Map<String, ?> model = request.model();
		assertEquals("Grade 3", model.get("appliedGrade"));
		assertEquals("ABCDEF12", model.get("applicationReference"));
		assertEquals(false, model.get("hasFeeReceipt"));
	}

	@Test
	void reissue_revokesThePreviousLetter() {
		Admission admission = approvedAdmission();
		admission.recordOfferLetter(5L);
		DocumentIssuance previous = issuance(5L);
		when(documentIssuanceService.findById(5L)).thenReturn(Optional.of(previous));
		when(documentIssuanceService.issue(any())).thenReturn(issuance(6L));

		new OfferLetterIssuer(documentIssuanceService).issue(1L, admission, 7L);

		verify(documentIssuanceService).revoke(previous, "Superseded by a re-issued offer letter");
	}

	@Test
	void firstIssue_revokesNothing() {
		when(documentIssuanceService.issue(any())).thenReturn(issuance(6L));

		new OfferLetterIssuer(documentIssuanceService).issue(1L, approvedAdmission(), 7L);

		verify(documentIssuanceService, never()).revoke(any(), any());
	}
}
