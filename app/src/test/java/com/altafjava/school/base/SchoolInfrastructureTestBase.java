package com.altafjava.school.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.altafjava.platform.testsupport.InfrastructureTest;
import com.altafjava.platform.testsupport.TestInfrastructureInitializer;
import com.altafjava.school.SchoolApplication;

/**
 * The real application on the {@code dev} profile, as a developer starts it: production main class,
 * demo seed, real Redis and RabbitMQ, and no test doubles.
 */
@InfrastructureTest
@SpringBootTest(classes = SchoolApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "school.demo.seed-on-startup=true")
@ContextConfiguration(initializers = TestInfrastructureInitializer.class)
@ActiveProfiles("dev")
public abstract class SchoolInfrastructureTestBase {
}
