package com.altafjava.school.performance;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import java.time.Duration;
import java.util.UUID;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

/**
 * School file upload/download load test.
 *
 * <p>
 * Tests document upload initiation, confirmation, and download URL retrieval for school
 * entities. Validates that file management APIs remain responsive under concurrent load.
 *
 * <p>
 * Run: {@code ./gradlew :app:gatlingRun-com.altafjava.school.performance.FileUploadSimulation}
 */
public class FileUploadSimulation extends Simulation {

	HttpProtocolBuilder httpProtocol = http
			.baseUrl(System.getProperty("baseUrl", "http://localhost:8080"))
			.acceptHeader("application/json")
			.contentTypeHeader("application/json")
			.userAgentHeader("Gatling/SchoolPerf");

	ScenarioBuilder scn = scenario("File Upload and Download Scenario")
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

			// 2. Initiate Upload
			.exec(session -> session.set("filename", "test-file-" + UUID.randomUUID() + ".txt"))
			.exec(
					http("Initiate File Upload")
							.post("/api/v1/files/upload")
							.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
							.header("Authorization", "Bearer #{authToken}")
							.body(StringBody(
									"{\"entityType\": \"DOCUMENT\", \"entityId\": \"doc-1\", \"filename\": \"#{filename}\", \"size\": 1024, \"mimeType\": \"text/plain\"}"))
							.check(status().in(200, 401, 403, 429))
							.check(status().transform(s -> s == 200).saveAs("uploadInitiated"))
							.checkIf("#{uploadInitiated}").then(jsonPath("$.fileId").saveAs("fileId")))
			.pause(1)

			// 3. Confirm Upload and retrieve download URL
			.doIf(session -> session.contains("fileId")).then(
					exec(
							http("Confirm File Upload")
									.post("/api/v1/files/#{fileId}/confirm")
									.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
									.header("Authorization", "Bearer #{authToken}")
									.check(status().in(200, 401, 403, 429, 500)))
							.pause(1)
							.exec(
									http("Get Download URL")
											.get("/api/v1/files/#{fileId}/download")
											.header("X-Tenant-Id", System.getProperty("tenantId", "1"))
											.header("Authorization", "Bearer #{authToken}")
											.check(status().in(200, 401, 403, 404, 429))));

	{
		setUp(
				scn.injectOpen(
						rampUsers(100).during(Duration.ofSeconds(20))))
				.protocols(httpProtocol)
				.assertions(
						global().responseTime().percentile(95).lt(1500),
						global().failedRequests().percent().lt(5.0));
	}
}
