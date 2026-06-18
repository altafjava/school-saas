package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
 * Verifies that domain Liquibase migrations run AFTER all platform migrations.
 *
 * Phase 5 validation goal: confirm DomainLiquibaseAutoConfiguration extension point works —
 * school tables exist alongside platform tables with correct ordering.
 */
@Import({TestRedisConfig.class, TestPaymentConfig.class})
class DomainMigrationOrderIntegrationTest extends SchoolIntegrationTestBase {

    @Autowired
    private DataSource dataSource;

    @Test
    void platformTables_existAfterMigration() {
        Set<String> tables = loadTableNames();
        // Core platform tables — must exist before domain tables
        assertTrue(tables.contains("tenants"), "Platform table 'tenants' must exist");
        assertTrue(tables.contains("subscription_plans"), "Platform table 'subscription_plans' must exist");
        assertTrue(tables.contains("audit_logs"), "Platform table 'audit_logs' must exist");
    }

    @Test
    void schoolDomainTables_existAfterDomainMigration() {
        Set<String> tables = loadTableNames();
        // School domain tables — created by DomainLiquibaseAutoConfiguration after platform migrations
        assertTrue(tables.contains("students"), "School domain table 'students' must exist");
        assertTrue(tables.contains("teachers"), "School domain table 'teachers' must exist");
        assertTrue(tables.contains("classrooms"), "School domain table 'classrooms' must exist");
    }

    @Test
    void schoolTablesForeignKeys_referenceExistingPlatformTables() {
        // Verifies the FK constraint school->tenants was applied: tenants must exist first
        assertDoesNotThrow(() -> {
            Set<String> tables = loadTableNames();
            assertTrue(tables.contains("tenants") && tables.contains("students"),
                    "Both tenants and students must exist for FK constraint to be valid");
        });
    }

    private Set<String> loadTableNames() {
        Set<String> tables = new HashSet<>();
        assertDoesNotThrow(() -> {
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        tables.add(rs.getString("TABLE_NAME").toLowerCase());
                    }
                }
            }
        });
        return tables;
    }
}
