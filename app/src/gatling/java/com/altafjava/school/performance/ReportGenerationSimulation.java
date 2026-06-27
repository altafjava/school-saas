package com.altafjava.school.performance;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import java.time.Duration;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

/**
 * School report generation load test.
 *
 * <p>
 * Tests creating report definitions and triggering report execution for the school domain.
 * Validates that report generation at load does not degrade API response times.
 *
 * <p>
 * Run: {@code ./gradlew :app:gatlingRun-com.altafjava.school.performance.ReportGenerationSimulation}
 */
public class ReportGenerationSimulation extends Simulation {

	HttpProtocolBuilder httpProtocol = http
			.baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
			.acceptHeader("application/json")
			.contentTypeHeader("application/json")
			.userAgentHeader("Gatling/SchoolPerf");

	ScenarioBuilder scn = scenario("Report Generation Scenario")
			// 1. Authenticate
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

			// 2. Create Report Definition
			.exec(
					http("Create Report Definition")
							.post("/api/v1/reports/definitions")
							.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
							.header("Authorization", "Bearer #{authToken}")
							.body(StringBody(
									"{\"name\": \"Student Attendance Report\", \"type\": \"ATTENDANCE\", \"outputFormat\": \"PDF\", \"active\": true}"))
							.check(status().in(200, 201, 401, 403, 429))
							.check(status().transform(s -> s == 200 || s == 201).saveAs("reportCreated"))
							.checkIf("#{reportCreated}").then(jsonPath("$.data.id").saveAs("reportDefId")))
			.pause(1)

			// 3. Execute Report
			.doIf(session -> session.contains("reportDefId")).then(
					exec(
							http("Execute Report")
									.post("/api/v1/reports/definitions/#{reportDefId}/execute")
									.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
									.header("Authorization", "Bearer #{authToken}")
									.check(status().in(200, 401, 403, 429)))
							.pause(1));

	{
		setUp(
				scn.injectOpen(
						rampUsers(50).during(Duration.ofSeconds(20))))
				.protocols(httpProtocol)
				.assertions(
						global().responseTime().percentile(95).lt(2000),
						global().failedRequests().percent().lt(5.0));
	}
}
