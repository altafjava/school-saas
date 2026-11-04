package com.altafjava.school.domain.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.ErrorCode;

/** School error codes are part of the API contract, so each must be usable and unique. */
class SchoolErrorCodesTest {

	@Test
	void everyCode_hasAUsableHttpStatusAndADescription() {
		for (ErrorCode code : SchoolErrorCodes.CATALOG) {
			assertTrue(code.httpStatus() >= 400 && code.httpStatus() < 600, code + " has status " + code.httpStatus());
			assertFalse(code.description().isBlank(), code + " has no description");
			assertEquals(((Enum<?>) code).name(), code.code());
		}
	}

	@Test
	void codes_areUniqueAcrossEverySchoolEnum() {
		List<String> codes = SchoolErrorCodes.CATALOG.stream().map(ErrorCode::code).toList();

		assertEquals(codes.size(), codes.stream().distinct().count());
	}
}
