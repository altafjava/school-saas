package com.altafjava.school.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.altafjava.school.SchoolTestApplication;

/**
 * Base class for all school integration tests.
 *
 * Starts a shared MariaDB TestContainer for the test suite.
 * All subclasses share the same container — started once, reused across test classes.
 *
 * Platform gap noted: the platform's test infrastructure (TestDatabaseContextInitializer,
 * TestRedisConfig, TestPaymentConfig) is not published as a test artifact. Platform should
 * expose a com.altafjava.platform:spring-boot-starter-test module for domain test reuse.
 */
@SpringBootTest(classes = SchoolTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class SchoolIntegrationTestBase {

	@Container
	static final MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.2.2")
			.withDatabaseName("school_test")
			.withUsername("root")
			.withPassword("mysql")
			.withReuse(true);

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
		registry.add("spring.datasource.username", mariaDB::getUsername);
		registry.add("spring.datasource.password", mariaDB::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
		// Redis not started — tests use in-memory mocks via TestRedisConfig
		registry.add("spring.data.redis.host", () -> "localhost");
		registry.add("spring.data.redis.port", () -> "6379");
		// Disable Quartz clustering for tests
		registry.add("spring.quartz.properties.org.quartz.jobStore.isClustered", () -> "false");
	}
}
