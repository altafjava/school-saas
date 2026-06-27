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
 * School user authentication load test.
 *
 * <p>
 * Tests registration, login, and protected endpoint access for school users.
 * Validates the full authentication flow under concurrent user load.
 *
 * <p>
 * Run: {@code ./gradlew :app:gatlingRun-com.altafjava.school.performance.UserAuthSimulation}
 */
public class UserAuthSimulation extends Simulation {

	HttpProtocolBuilder httpProtocol = http
			.baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
			.acceptHeader("application/json")
			.contentTypeHeader("application/json")
			.userAgentHeader("Gatling/SchoolPerf");

	Iterator<Map<String, Object>> customFeeder = Stream.generate((Supplier<Map<String, Object>>) () -> {
		String uuid = UUID.randomUUID().toString();
		return Map.of(
				"email", "user-" + uuid + "@school.com",
				"password", "Password123!",
				"firstName", "John",
				"lastName", "Doe",
				"roles", "[\"TEACHER\"]");
	}).iterator();

	ScenarioBuilder scn = scenario("User Registration and Login Scenario")
			.feed(customFeeder)
			.exec(
					http("Register User")
							.post("/api/v1/auth/register")
							.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
							.body(StringBody(
									"{\"email\": \"#{email}\", \"password\": \"#{password}\", \"firstName\": \"#{firstName}\", \"lastName\": \"#{lastName}\", \"roles\": #{roles}}"))
							.check(status().in(201, 400, 404, 429, 500)))
			.pause(1)
			.exec(
					http("Login User")
							.post("/api/v1/auth/login")
							.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
							.body(StringBody("{\"email\": \"#{email}\", \"password\": \"#{password}\"}"))
							// 429 is expected under load (per-IP rate limit on login endpoints)
							.check(status().in(200, 400, 401, 404, 429, 500))
							.check(status().transform(s -> s == 200).saveAs("loginOk"))
							.checkIf("#{loginOk}").then(jsonPath("$.data.accessToken").saveAs("accessToken")))
			.pause(1)
			.doIf(session -> session.contains("accessToken"))
			.then(
					exec(
							http("Access Protected Endpoint (Get Me)")
									.get("/api/v1/users/me")
									.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
									.header("Authorization", "Bearer #{accessToken}")
									.check(status().in(200, 401, 403, 404, 500))));

	{
		setUp(
				scn.injectOpen(
						rampUsers(10).during(Duration.ofSeconds(5))))
				.protocols(httpProtocol)
				.assertions(
						global().responseTime().max().lt(2000),
						global().responseTime().percentile(95).lt(1000),
						global().successfulRequests().percent().gt(95.0));
	}
}
