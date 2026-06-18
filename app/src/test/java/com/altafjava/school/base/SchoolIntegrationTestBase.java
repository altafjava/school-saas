package com.altafjava.school.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.altafjava.school.SchoolTestApplication;

/**
 * Base class for all school integration tests.
 *
 * Uses the local MariaDB instance configured in application-test.yml.
 * No Docker or Testcontainers required — run MariaDB locally before executing tests.
 */
@SpringBootTest(classes = SchoolTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class SchoolIntegrationTestBase {
}
