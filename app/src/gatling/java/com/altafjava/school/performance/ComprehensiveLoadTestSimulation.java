package com.altafjava.school.performance;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import java.time.Duration;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

/**
 * Comprehensive school application load test with configurable test profiles.
 *
 * <p>
 * Supports multiple test types selectable via the {@code -DtestType=} system property:
 * <ul>
 * <li>{@code stress} (default) — ramp up to find the breaking point</li>
 * <li>{@code spike} — sudden large load increase</li>
 * <li>{@code soak} — sustained load over an extended period</li>
 * <li>{@code scalability} — step load to test horizontal scaling</li>
 * </ul>
 *
 * <p>
 * Run:
 * {@code ./gradlew :app:gatlingRun-com.altafjava.school.performance.ComprehensiveLoadTestSimulation -DtestType=soak}
 */
public class ComprehensiveLoadTestSimulation extends Simulation {

	HttpProtocolBuilder httpProtocol = http
			.baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
			.acceptHeader("application/json")
			.contentTypeHeader("application/json")
			.userAgentHeader("Gatling/SchoolPerf");

	ScenarioBuilder standardScn = scenario("Standard API Load")
			.exec(
					http("Health Check")
							.get("/actuator/health")
							.check(status().in(200, 401, 403, 404, 429, 503)));

	{
		String testType = System.getProperty("testType", "stress");

		switch (testType) {
			case "stress":
				// Stress testing: find breaking points by ramping load until the system fails
				setUp(
						standardScn.injectOpen(
								rampUsersPerSec(10).to(100).during(Duration.ofMinutes(2))))
						.protocols(httpProtocol);
				break;

			case "spike":
				// Spike testing: sudden load increase
				setUp(
						standardScn.injectOpen(
								nothingFor(Duration.ofSeconds(10)),
								atOnceUsers(500),
								nothingFor(Duration.ofSeconds(20)),
								atOnceUsers(1000)))
						.protocols(httpProtocol);
				break;

			case "soak":
				// Soak testing: sustained load over time
				setUp(
						standardScn.injectOpen(
								constantUsersPerSec(20).during(Duration.ofHours(1))))
						.protocols(httpProtocol);
				break;

			case "scalability":
				// Scalability testing: step load to test horizontal scaling
				setUp(
						standardScn.injectOpen(
								incrementUsersPerSec(10)
										.times(5)
										.eachLevelLasting(Duration.ofSeconds(30))
										.separatedByRampsLasting(Duration.ofSeconds(10))
										.startingFrom(10)))
						.protocols(httpProtocol);
				break;

			default:
				// Smoke test — minimal load to verify the system is alive
				setUp(
						standardScn.injectOpen(
								atOnceUsers(5)))
						.protocols(httpProtocol);
		}
	}
}
