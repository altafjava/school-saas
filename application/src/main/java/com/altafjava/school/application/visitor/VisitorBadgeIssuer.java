package com.altafjava.school.application.visitor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.application.service.FileStorageService;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.visitor.model.VisitorRequest;
import lombok.RequiredArgsConstructor;

/**
 * Issues the badge a visitor wears — through the Document Template Engine like every ID card — so
 * a guard scanning its QR sees whether it is genuine and still valid. The badge belongs to the
 * visit request: it exists before the visit's log row does.
 */
@Component
@RequiredArgsConstructor
public class VisitorBadgeIssuer {

	private final DocumentIssuanceService documentIssuanceService;
	private final FileStorageService fileStorageService;

	public DocumentIssuance issue(Long tenantId, VisitorRequest request, Employee host, UUID photoFilePublicId,
			LocalDate validOn, Long issuedByUserId) {
		String hostName = host.getFirstName() + " " + host.getLastName();
		Map<String, Object> model = new HashMap<>();
		model.put("visitorName", request.getVisitorName());
		model.put("hostName", hostName);
		model.put("purpose", request.getPurpose());
		model.put("validOn", validOn.toString());
		model.put("photo", fileStorageService.downloadFileForTenant(photoFilePublicId.toString()));
		return documentIssuanceService.issue(new DocumentIssueRequest(tenantId, SchoolDocumentTypes.VISITOR_BADGE,
				SchoolDocumentTypes.OWNER_VISITOR_REQUEST, request.getId(), "Visitor Badge", request.getVisitorName(),
				model, issuedByUserId));
	}
}
