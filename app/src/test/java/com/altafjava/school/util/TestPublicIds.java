package com.altafjava.school.util;

import jakarta.persistence.Table;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Looks up the public id of a record a test only holds the internal id of, so request bodies can
 * name records the way API clients must.
 */
@Component
public class TestPublicIds {

	private final JdbcTemplate jdbcTemplate;

	public TestPublicIds(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	// Pass Employee.class for a teacher: the public id lives on the employee row.
	public String of(Class<?> entityClass, Long id) {
		String table = entityClass.getAnnotation(Table.class).name();
		return jdbcTemplate.queryForObject("SELECT public_id FROM " + table + " WHERE id = ?", String.class, id);
	}
}
