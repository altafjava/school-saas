package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;

/**
 * Verifies that all six extended school domain tables are created by the second Liquibase changeset.
 *
 * Phase 5 validation: DomainLiquibaseAutoConfiguration runs changesets 002+ after changeset 001.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class ExtendedDomainMigrationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private DataSource dataSource;

	@Test
	void extendedSchoolTables_existAfterMigration() {
		Set<String> tables = loadTableNames();
		assertTrue(tables.contains("academic_years"), "Table 'academic_years' must exist");
		assertTrue(tables.contains("exams"), "Table 'exams' must exist");
		assertTrue(tables.contains("attendance"), "Table 'attendance' must exist");
		assertTrue(tables.contains("grades"), "Table 'grades' must exist");
		assertTrue(tables.contains("fee_structures"), "Table 'fee_structures' must exist");
		assertTrue(tables.contains("fee_payments"), "Table 'fee_payments' must exist");
	}

	@Test
	void allSchoolTables_existAfterMigration() {
		Set<String> tables = loadTableNames();
		// Original tables
		assertTrue(tables.contains("students"));
		assertTrue(tables.contains("teachers"));
		assertTrue(tables.contains("classrooms"));
		// Extended tables
		assertTrue(tables.contains("academic_years"));
		assertTrue(tables.contains("exams"));
		assertTrue(tables.contains("attendance"));
		assertTrue(tables.contains("grades"));
		assertTrue(tables.contains("fee_structures"));
		assertTrue(tables.contains("fee_payments"));
	}

	private Set<String> loadTableNames() {
		Set<String> tables = new HashSet<>();
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData metaData = conn.getMetaData();
			try (ResultSet rs = metaData.getTables(null, null, "%", new String[] { "TABLE" })) {
				while (rs.next()) {
					tables.add(rs.getString("TABLE_NAME").toLowerCase());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to load table names", e);
		}
		return tables;
	}
}
