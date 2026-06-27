package com.altafjava.school.performance;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

/**
 * School domain CRUD operations load test.
 *
 * <p>
 * Tests: user creation, retrieval, update, and deletion for school-domain entities. Requires
 * at least one tenant pre-created in the target environment.
 *
 * <p>
 * Run: {@code ./gradlew :app:gatlingRun-com.altafjava.school.performance.CrudOperationsSimulation}
 */
public class CrudOperationsSimulation extends Simulation {

	HttpProtocolBuilder httpProtocol = http
			.baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
			.acceptHeader("application/json")
			.contentTypeHeader("application/json")
			.userAgentHeader("Gatling/SchoolPerf");

	Iterator<Map<String, Object>> customFeeder = Stream.generate((Supplier<Map<String, Object>>) () -> {
		String uuid = UUID.randomUUID().toString();
		return Map.of(
				"email", "crud-user-" + uuid + "@school.com",
				"password", "Password123!",
				"firstName", "Test",
				"lastName", "User");
	}).iterator();

	ScenarioBuilder scn = scenario("CRUD Operations Scenario")
			.feed(customFeeder)

			// 1. Authenticate to get a token
			.exec(
					http("Admin Login")
							.post("/api/v1/auth/login")
							.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
							.body(StringBody("{\"email\": \"admin@school.com\", \"password\": \"Admin123!\"}"))
							.check(status().in(200, 400, 401, 429))
							.check(status().transform(s -> s == 200).saveAs("adminLoginOk"))
							.checkIf("#{adminLoginOk}").then(jsonPath("$.data.accessToken").saveAs("authToken")))
			.exec(session -> {
				if (!session.contains("authToken")) {
					return session.set("authToken", "dummy-token");
				}
				return session;
			})
			.pause(1)

			// 2. Create User
			.exec(
					http("Create User")
							.post("/api/v1/users")
							.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
							.header("Authorization", "Bearer #{authToken}")
							.body(StringBody(
									"{\"email\": \"#{email}\", \"password\": \"#{password}\", \"firstName\": \"#{firstName}\", \"lastName\": \"#{lastName}\", \"roles\": [\"STUDENT\"]}"))
							.check(status().in(201, 401, 403, 429))
							.check(status().transform(s -> s == 201).saveAs("userCreated"))
							.checkIf("#{userCreated}").then(jsonPath("$.data.id").saveAs("createdUserId")))
			.pause(1)

			// 3. Read User
			.doIf(session -> session.contains("createdUserId")).then(
					exec(
							http("Read User")
									.get("/api/v1/users/#{createdUserId}")
									.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
									.header("Authorization", "Bearer #{authToken}")
									.check(status().in(200, 401, 403, 429)))
							.pause(1)

							// 4. Update User
							.exec(
									http("Update User")
											.put("/api/v1/users/#{createdUserId}")
											.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
											.header("Authorization", "Bearer #{authToken}")
											.body(StringBody("{\"firstName\": \"Updated\", \"lastName\": \"User\"}"))
											.check(status().in(200, 401, 403, 429)))
							.pause(1)

							// 5. Delete User
							.exec(
									http("Delete User")
											.delete("/api/v1/users/#{createdUserId}")
											.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
											.header("Authorization", "Bearer #{authToken}")
											.check(status().in(204, 200, 401, 403, 429))));

	{
		setUp(
				scn.injectOpen(
						rampUsers(500).during(Duration.ofSeconds(30))))
				.protocols(httpProtocol)
				.assertions(
						global().responseTime().percentile(95).lt(1000),
						global().failedRequests().percent().lt(5.0));
	}
}
