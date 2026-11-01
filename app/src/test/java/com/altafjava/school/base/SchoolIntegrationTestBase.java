package com.altafjava.school.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.altafjava.platform.testsupport.TestInfrastructureInitializer;
import com.altafjava.school.SchoolTestApplication;

/** Base class for all school integration tests. Runs against the shared test containers. */
@SpringBootTest(classes = SchoolTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TestInfrastructureInitializer.class)
@ActiveProfiles("test")
public abstract class SchoolIntegrationTestBase {
}
