package com.altafjava.school.application.idcard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class QrPayloadReaderTest {

	private final QrPayloadReader reader = new QrPayloadReader();

	@Test
	void verificationCodeOf_aBareCode_isReturnedAsIs() {
		assertEquals("a1B2c3D4", reader.verificationCodeOf("a1B2c3D4"));
	}

	@Test
	void verificationCodeOf_aVerificationLink_isItsLastPathSegment() {
		assertEquals("a1B2c3D4",
				reader.verificationCodeOf("https://greenfield.school.example/api/v1/documents/verify/a1B2c3D4"));
	}

	@Test
	void verificationCodeOf_aLinkWithTrailingSlashQueryAndFragment_stillFindsTheCode() {
		assertEquals("a1B2c3D4", reader.verificationCodeOf(" https://x.example/verify/a1B2c3D4/?lang=en#top "));
	}

	@Test
	void verificationCodeOf_urlSafeBase64Characters_areAccepted() {
		assertEquals("ab-_Z9xY", reader.verificationCodeOf("ab-_Z9xY"));
	}

	@Test
	void verificationCodeOf_somethingThatIsNotACode_isRefused() {
		List<String> notCodes = List.of("", "   ", "https://x.example/verify/", "https://x.example", "short",
				"has space", "semi;colon",
				"../etc/passwd", "https://x.example/verify/abc%20def", "<script>alert(1)</script>");

		for (String payload : notCodes) {
			assertThrows(BusinessException.class, () -> reader.verificationCodeOf(payload), payload);
		}
	}

	@Test
	void verificationCodeOf_nullOrOversizedPayload_isRefused() {
		assertThrows(BusinessException.class, () -> reader.verificationCodeOf(null));
		assertThrows(BusinessException.class, () -> reader.verificationCodeOf("a".repeat(2049)));
	}

	@Test
	void verificationCodeOf_aCodeLongerThanAnyIssuedOne_isRefused() {
		assertThrows(BusinessException.class, () -> reader.verificationCodeOf("a".repeat(33)));
	}
}
