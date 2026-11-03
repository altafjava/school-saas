package com.altafjava.school.application.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.common.model.Gender;

class ImportRowTest {

	private final ImportRow row = new ImportRow(2, Map.of("name", "  Ada  ", "blank", " ", "born", "2015-04-30",
			"bad", "30/04/2015", "fee", "2500.50", "yes", "Yes", "gender", "female"));

	@Test
	void text_isTrimmed_andBlankIsAbsent() {
		assertEquals("Ada", row.text("name"));
		assertNull(row.text("blank"));
		assertNull(row.text("missing"));
	}

	@Test
	void requiredText_whenBlank_namesTheField() {
		ImportRowException error = assertThrows(ImportRowException.class, () -> row.requiredText("blank"));

		assertEquals("blank", error.getField());
	}

	@Test
	void date_readsIsoDates_andExplainsOthers() {
		assertEquals(LocalDate.of(2015, 4, 30), row.date("born"));
		ImportRowException error = assertThrows(ImportRowException.class, () -> row.date("bad"));
		assertTrue(error.getMessage().contains("2026-04-30"));
	}

	@Test
	void requiredAmount_readsMoney_andRefusesNonsense() {
		assertEquals(0, new BigDecimal("2500.50").compareTo(row.requiredAmount("fee")));
		assertThrows(ImportRowException.class, () -> new ImportRow(2, Map.of("fee", "-1")).requiredAmount("fee"));
		assertThrows(ImportRowException.class, () -> new ImportRow(2, Map.of("fee", "1.234")).requiredAmount("fee"));
		assertThrows(ImportRowException.class, () -> new ImportRow(2, Map.of("fee", "lots")).requiredAmount("fee"));
	}

	@Test
	void flag_andChoice_areForgivingAboutCase() {
		assertTrue(row.flag("yes"));
		assertFalse(row.flag("missing"));
		assertEquals(Gender.FEMALE, row.choice("gender", Gender.class, Gender.NOT_SPECIFIED));
		assertEquals(Gender.NOT_SPECIFIED, row.choice("missing", Gender.class, Gender.NOT_SPECIFIED));
		assertThrows(ImportRowException.class, () -> row.choice("name", Gender.class, null));
	}
}
