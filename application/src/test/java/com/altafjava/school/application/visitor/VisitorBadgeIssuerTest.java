package com.altafjava.school.application.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.application.service.FileStorageService;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.visitor.model.VisitorRequest;

@ExtendWith(MockitoExtension.class)
class VisitorBadgeIssuerTest {

	@Mock
	private DocumentIssuanceService documentIssuanceService;
	@Mock
	private FileStorageService fileStorageService;

	@Test
	@SuppressWarnings("unchecked")
	void issue_ownsTheBadgeByTheRequestAndPrintsTheVisitorHostAndPhoto() {
		UUID photoId = UUID.randomUUID();
		byte[] photo = { 1, 2, 3 };
		VisitorRequest request = VisitorRequest.walkIn("Alex Ray", null, "Delivery", 20L, 3L, LocalDate.now());
		request.setId(40L);
		Employee host = Employee.create(StaffCategory.SUPPORT, "EMP-1", "Jane", "Doe", "jane@school.test", null);
		LocalDate today = LocalDate.of(2026, 10, 8);
		DocumentIssuance issued = DocumentIssuance.create("VISITOR_BADGE", "VISITOR_REQUEST", 40L, "Visitor Badge",
				"Alex Ray", null, null, "code1234", "key", 5L);
		when(fileStorageService.downloadFileForTenant(photoId.toString())).thenReturn(photo);
		when(documentIssuanceService.issue(any(DocumentIssueRequest.class))).thenReturn(issued);

		DocumentIssuance badge = new VisitorBadgeIssuer(documentIssuanceService, fileStorageService).issue(1L,
				request, host, photoId, today, 5L);

		assertSame(issued, badge);
		ArgumentCaptor<DocumentIssueRequest> captor = ArgumentCaptor.forClass(DocumentIssueRequest.class);
		verify(documentIssuanceService).issue(captor.capture());
		DocumentIssueRequest issuedRequest = captor.getValue();
		assertEquals(SchoolDocumentTypes.VISITOR_BADGE, issuedRequest.documentType());
		assertEquals(SchoolDocumentTypes.OWNER_VISITOR_REQUEST, issuedRequest.ownerEntityType());
		assertEquals(40L, issuedRequest.ownerEntityId());
		Map<String, Object> model = (Map<String, Object>) issuedRequest.model();
		assertEquals("Alex Ray", model.get("visitorName"));
		assertEquals("Jane Doe", model.get("hostName"));
		assertEquals("2026-10-08", model.get("validOn"));
		assertSame(photo, model.get("photo"));
	}
}
