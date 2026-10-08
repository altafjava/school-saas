package com.altafjava.school.api.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.Sort;
import com.altafjava.platform.core.exception.BusinessException;

/**
 * Turns {@code sort=field[,asc|desc]} query values into a {@link Sort}, rejecting any attribute not
 * on the endpoint's allow-list. A sorted page always ends on {@code id}, so rows that tie on the
 * chosen columns still come back in a stable order from one page to the next.
 */
public final class SortParser {

	public static final Set<String> AUDIT_ATTRIBUTES = Set.of("createdAt", "updatedAt");
	static final int MAX_SORT_KEYS = 3;
	private static final String TIE_BREAKER = "id";

	private SortParser() {
	}

	public static Sort parse(List<String> requested, Set<String> endpointAttributes) {
		if (requested == null || requested.isEmpty()) {
			return Sort.unsorted();
		}
		if (requested.size() > MAX_SORT_KEYS) {
			throw new BusinessException("Sort by at most " + MAX_SORT_KEYS + " fields");
		}
		Set<String> allowed = new TreeSet<>(AUDIT_ATTRIBUTES);
		allowed.addAll(endpointAttributes);
		Set<String> seen = new HashSet<>();
		List<Sort.Order> orders = new ArrayList<>();
		for (String value : requested) {
			Sort.Order order = toOrder(value, allowed);
			if (!seen.add(order.getProperty())) {
				throw new BusinessException("Sort field '" + order.getProperty() + "' is given more than once");
			}
			orders.add(order);
		}
		orders.add(Sort.Order.asc(TIE_BREAKER));
		return Sort.by(orders);
	}

	private static Sort.Order toOrder(String value, Set<String> allowed) {
		String[] parts = value.split(",", -1);
		String attribute = parts[0].strip();
		if (parts.length > 2 || !allowed.contains(attribute)) {
			throw new BusinessException(
					"Cannot sort by '" + value + "'. Sortable fields: " + String.join(", ", allowed));
		}
		if (parts.length == 1) {
			return Sort.Order.asc(attribute);
		}
		return switch (parts[1].strip().toLowerCase(Locale.ROOT)) {
			case "asc" -> Sort.Order.asc(attribute);
			case "desc" -> Sort.Order.desc(attribute);
			default -> throw new BusinessException("Sort direction must be asc or desc, was '" + parts[1] + "'");
		};
	}
}
