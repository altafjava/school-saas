package com.altafjava.school.application.admission;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.domain.admission.model.Admission;
import lombok.RequiredArgsConstructor;

/**
 * Issues an admission's offer letter through the Document Template Engine. Issuing again (a
 * corrected name, a new grade) revokes the previous letter so only the current one verifies as valid.
 * Persisting the new issuance id on the admission is the caller's job.
 */
@Component
@RequiredArgsConstructor
public class OfferLetterIssuer {

	private final DocumentIssuanceService documentIssuanceService;

	public DocumentIssuance issue(Long tenantId, Admission admission, Long issuedByUserId) {
		String applicantName = admission.getApplicantFirstName() + " " + admission.getApplicantLastName();
		Map<String, Object> model = new HashMap<>();
		model.put("applicantName", applicantName);
		model.put("guardianName", admission.getGuardianFirstName() + " " + admission.getGuardianLastName());
		model.put("appliedGrade", admission.getAppliedGrade());
		model.put("applicationReference", admission.getPublicId().toString().substring(0, 8).toUpperCase());
		model.put("offerDate", LocalDate.now().toString());
		model.put("hasFeeReceipt", admission.getApplicationFeeReceiptNumber() != null);
		model.put("feeReceiptNumber", admission.getApplicationFeeReceiptNumber());

		DocumentIssuance previous = admission.getOfferLetterIssuanceId() == null ? null
				: documentIssuanceService.findById(admission.getOfferLetterIssuanceId()).orElse(null);
		DocumentIssuance issued = documentIssuanceService.issue(new DocumentIssueRequest(tenantId,
				SchoolDocumentTypes.ADMISSION_OFFER_LETTER, SchoolDocumentTypes.OWNER_ADMISSION, admission.getId(),
				"Admission Offer Letter", applicantName, model, issuedByUserId));
		if (previous != null && !previous.isRevoked()) {
			documentIssuanceService.revoke(previous, "Superseded by a re-issued offer letter");
		}
		return issued;
	}
}
