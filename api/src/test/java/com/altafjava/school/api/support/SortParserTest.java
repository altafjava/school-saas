package com.altafjava.school.api.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import com.altafjava.platform.core.exception.BusinessException;

class SortParserTest {

	private static final Set<String> ENDPOINT = Set.of("lastName", "firstName");

	@Test
	void parse_nothingRequested_isUnsorted() {
		assertTrue(SortParser.parse(null, ENDPOINT).isUnsorted());
		assertTrue(SortParser.parse(List.of(), ENDPOINT).isUnsorted());
	}

	@Test
	void parse_aFieldAlone_sortsAscendingAndEndsOnId() {
		Sort sort = SortParser.parse(List.of("lastName"), ENDPOINT);

		assertEquals(List.of(Sort.Order.asc("lastName"), Sort.Order.asc("id")), sort.toList());
	}

	@Test
	void parse_directionsAndSeveralFields_keepTheirOrder() {
		Sort sort = SortParser.parse(List.of("lastName,desc", "firstName,ASC"), ENDPOINT);

		assertEquals(List.of(Sort.Order.desc("lastName"), Sort.Order.asc("firstName"), Sort.Order.asc("id")),
				sort.toList());
	}

	@Test
	void parse_auditColumnsAreAlwaysAllowed() {
		Sort sort = SortParser.parse(List.of("createdAt,desc"), Set.of());

		assertEquals(Sort.Order.desc("createdAt"), sort.toList().get(0));
	}

	@Test
	void parse_aFieldTheEndpointDoesNotDeclare_isRefusedAndTheAllowedOnesAreListed() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> SortParser.parse(List.of("passwordHash"), ENDPOINT));

		assertTrue(ex.getMessage().contains("firstName"));
		assertTrue(ex.getMessage().contains("createdAt"));
	}

	@Test
	void parse_nestedOrInjectedAttributePaths_areRefused() {
		for (String hostile : List.of("address.country", "firstName; DROP TABLE students", "id", "(select 1)", "")) {
			assertThrows(BusinessException.class, () -> SortParser.parse(List.of(hostile), ENDPOINT), hostile);
		}
	}

	@Test
	void parse_anUnknownDirection_isRefused() {
		assertThrows(BusinessException.class, () -> SortParser.parse(List.of("lastName,sideways"), ENDPOINT));
		assertThrows(BusinessException.class, () -> SortParser.parse(List.of("lastName,asc,extra"), ENDPOINT));
	}

	@Test
	void parse_aRepeatedField_isRefused() {
		assertThrows(BusinessException.class, () -> SortParser.parse(List.of("lastName", "lastName,desc"), ENDPOINT));
	}

	@Test
	void parse_moreThanThreeFields_isRefused() {
		assertThrows(BusinessException.class, () -> SortParser.parse(
				List.of("lastName", "firstName", "createdAt", "updatedAt"), ENDPOINT));
	}
}
