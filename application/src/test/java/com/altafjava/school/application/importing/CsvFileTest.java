package com.altafjava.school.application.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class CsvFileTest {

	@Test
	void parse_readsTheHeaderAndRows_includingQuotedCommas() {
		CsvFile file = CsvFile.parse("name,city\nAda,\"Pune, MH\"\n\nBen\n");

		assertEquals(List.of("name", "city"), file.headers());
		assertEquals(2, file.rows().size());
		assertEquals("Pune, MH", file.value(file.rows().get(0), "city"));
		assertNull(file.value(file.rows().get(1), "city"));
	}

	@Test
	void parse_anEmptyFile_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> CsvFile.parse(""));
	}

	@Test
	void parse_tooManyRows_throwsBusinessException() {
		String content = "name\n" + "x\n".repeat(CsvFile.MAX_ROWS + 1);

		assertThrows(BusinessException.class, () -> CsvFile.parse(content));
	}
}
