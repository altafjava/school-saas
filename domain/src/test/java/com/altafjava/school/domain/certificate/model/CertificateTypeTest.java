package com.altafjava.school.domain.certificate.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class CertificateTypeTest {

	@Test
	void create_setsFieldsAndDefaultsActive() {
		CertificateType type = CertificateType.create("BONAFIDE", "Bonafide Certificate",
				"This is to certify that {{studentName}} is a bonafide student.");

		assertEquals("BONAFIDE", type.getCode());
		assertEquals("Bonafide Certificate", type.getName());
		assertTrue(type.isActive());
	}

	@Test
	void documentType_isTheCodeUnderTheCertificateFamily() {
		assertEquals("CERTIFICATE.TRANSFER", CertificateType.create("TRANSFER", "TC", "body").documentType());
	}

	@Test
	void create_rejectsCodesThatAreNotValidDocumentTypeSegments() {
		for (String code : new String[] { "bonafide", "X", "1ABC", "BONA-FIDE", "BONA.FIDE", null }) {
			assertThrows(BusinessException.class, () -> CertificateType.create(code, "n", "w"), String.valueOf(code));
		}
	}

	@Test
	void updateDetails_replacesNameAndWording() {
		CertificateType type = CertificateType.create("TC", "Old", "Old wording");

		type.updateDetails("New", "New wording {{studentName}}");

		assertEquals("New", type.getName());
		assertEquals("New wording {{studentName}}", type.getWording());
	}

	@Test
	void deactivateThenActivate_flipsActiveFlag() {
		CertificateType type = CertificateType.create("TC", "TC", "body");

		type.deactivate();
		assertFalse(type.isActive());
		type.activate();
		assertTrue(type.isActive());
	}
}
