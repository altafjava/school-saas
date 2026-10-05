package com.altafjava.school.domain.document.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class StudentDocumentTest {

	@Test
	void forStudent_setsStudentIdAndPendingStatus() {
		StudentDocument document = StudentDocument.forStudent(10L, "BIRTH_CERTIFICATE", UUID.randomUUID());

		assertEquals(10L, document.getStudentId());
		assertEquals(DocumentVerificationStatus.PENDING, document.getVerificationStatus());
	}

	@Test
	void forStudent_withNullStudentId_throws() {
		assertThrows(BusinessException.class,
				() -> StudentDocument.forStudent(null, "BIRTH_CERTIFICATE", UUID.randomUUID()));
	}

	@Test
	void forAdmission_withNullAdmissionId_throws() {
		assertThrows(BusinessException.class,
				() -> StudentDocument.forAdmission(null, "BIRTH_CERTIFICATE", UUID.randomUUID()));
	}

	@Test
	void verify_setsVerifiedFields() {
		StudentDocument document = StudentDocument.forStudent(10L, "BIRTH_CERTIFICATE", UUID.randomUUID());

		document.verify(5L);

		assertEquals(DocumentVerificationStatus.VERIFIED, document.getVerificationStatus());
		assertEquals(5L, document.getVerifiedByUserId());
	}

	@Test
	void reject_withoutReason_throws() {
		StudentDocument document = StudentDocument.forStudent(10L, "BIRTH_CERTIFICATE", UUID.randomUUID());

		assertThrows(BusinessException.class, () -> document.reject(5L, ""));
	}

	@Test
	void reject_withReason_setsRejectedFields() {
		StudentDocument document = StudentDocument.forStudent(10L, "BIRTH_CERTIFICATE", UUID.randomUUID());

		document.reject(5L, "Illegible scan");

		assertEquals(DocumentVerificationStatus.REJECTED, document.getVerificationStatus());
		assertEquals("Illegible scan", document.getRejectionReason());
	}

	@Test
	void verify_alreadyDecided_throws() {
		StudentDocument document = StudentDocument.forStudent(10L, "BIRTH_CERTIFICATE", UUID.randomUUID());
		document.verify(5L);

		assertThrows(BusinessException.class, () -> document.verify(5L));
	}
}
