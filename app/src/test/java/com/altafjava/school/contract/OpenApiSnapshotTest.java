package com.altafjava.school.contract;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Detects unreviewed drift in the public REST API surface with no frontend yet built to notice it
 * (see {@code TESTING.md} §4). Fetches the live OpenAPI spec and structurally compares it (via
 * {@link JsonNode} equality — key-order-insensitive, unlike a raw string diff) against a
 * committed baseline. A failure here means the API shape changed; update the baseline in the same
 * PR as a deliberate, reviewed decision, never as an incidental side effect.
 *
 * <p>
 * Two properties are overridden here only: {@code security.swagger.public} — platform-saas locks
 * {@code /v3/api-docs} to SUPER_ADMIN outside dev by default (a deliberate hardening from the
 * 2026-08-14 review) — and {@code springdoc.api-docs.enabled}, which the platform's own
 * {@code platform-defaults.yml} disables by default ({@code SWAGGER_ENABLED:false}); school-saas's
 * {@code application-dev.yml} re-enables it for local dev, but the {@code test} profile inherits
 * the platform default, so this test needs its own override rather than the app's ambient config.
 *
 * <p>
 * To regenerate the baseline after an intentional API change, run this test once with
 * {@code -DupdateOpenApiSnapshot=true} and commit the rewritten
 * {@code src/test/resources/openapi-snapshot.json}.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestPropertySource(properties = { "security.swagger.public=true", "springdoc.api-docs.enabled=true" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenApiSnapshotTest extends SchoolIntegrationTestBase {

	private static final String SNAPSHOT_RESOURCE = "/openapi-snapshot.json";

	@LocalServerPort
	int port;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void openApiSpec_matchesCommittedSnapshot() throws IOException {
		RestAssured.port = port;
		RestAssured.basePath = "";

		// platform-defaults.yml customizes springdoc's path from its own default (/v3/api-docs) to
		// /api-docs — see the class Javadoc.
		String liveSpecJson = given()
				.contentType(ContentType.JSON)
				.when()
				.get("/api-docs")
				.then()
				.extract().asString();

		JsonNode liveSpec = stripEphemeralFields(objectMapper.readTree(liveSpecJson));

		if (Boolean.getBoolean("updateOpenApiSnapshot")) {
			java.nio.file.Path target = java.nio.file.Path.of(
					"src/test/resources/openapi-snapshot.json");
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), liveSpec);
			fail("Snapshot regenerated at " + target.toAbsolutePath()
					+ " — review the diff, commit it, and re-run without -DupdateOpenApiSnapshot");
		}

		JsonNode expectedSpec = loadSnapshot();
		assertEquals(expectedSpec, liveSpec,
				"OpenAPI spec no longer matches the committed snapshot (src/test/resources/openapi-snapshot.json). "
						+ "If this change is intentional, regenerate it with "
						+ "'-DupdateOpenApiSnapshot=true' and commit the update.");
	}

	@Test
	void operationIds_areUniqueAndStable() {
		java.util.Set<String> seen = new java.util.HashSet<>();
		liveSpec().path("paths").properties().forEach(path -> path.getValue().properties().forEach(op -> {
			JsonNode id = op.getValue().path("operationId");
			if (id.isMissingNode()) {
				return;
			}
			String operationId = id.asString();
			assertTrue(seen.add(operationId), "duplicate operationId " + operationId);
			assertTrue(!operationId.matches(".*_\\d+$"),
					"operationId " + operationId + " carries springdoc's unstable numeric suffix");
		}));
		assertTrue(seen.size() > 500, "expected the school's operations, found " + seen.size());
	}

	@Test
	void everyOperation_documentsTypedErrorResponses() {
		JsonNode spec = liveSpec();
		assertTrue(spec.path("components").path("schemas").has("ApiErrorResponse"));
		spec.path("paths").properties().forEach(path -> path.getValue().properties().forEach(op -> {
			JsonNode responses = op.getValue().path("responses");
			if (!responses.isMissingNode()) {
				assertTrue(responses.has("500"), path.getKey() + " " + op.getKey() + " lacks a typed 500 response");
			}
		}));
	}

	@Test
	void everySuccessResponse_declaresItsPayloadType() {
		liveSpec().path("paths").properties().forEach(path -> path.getValue().properties().forEach(op -> {
			op.getValue().path("responses").properties().stream().filter(response -> response.getKey().startsWith("2"))
					.forEach(response -> response.getValue().path("content").properties().forEach(media -> assertFalse(
							media.getValue().path("schema").path("$ref").asString("").endsWith("/ApiResponse"),
							path.getKey() + " " + op.getKey() + " returns the untyped ApiResponse envelope — "
									+ "drop the explicit @Schema(implementation = ApiResponse.class) so the payload type is inferred")));
		}));
	}

	@Test
	void structuredResponses_areDeclaredAsJson() {
		liveSpec().path("paths").properties().forEach(path -> path.getValue().properties().forEach(op -> op.getValue()
				.path("responses").properties().forEach(response -> assertFalse(
						response.getValue().path("content").path("*/*").path("schema").has("$ref"),
						path.getKey() + " " + op.getKey() + " " + response.getKey()
								+ " declares a structured body as */* — generators only read application/json"))));
	}

	@Test
	void noSchemaCarriesABlankDefault() {
		assertNoBlankDefault(liveSpec(), "#");
	}

	private void assertNoBlankDefault(JsonNode node, String path) {
		if (node.isObject()) {
			JsonNode value = node.get("default");
			assertFalse(value != null && (value.isNull() || value.isString() && value.asString().isEmpty()),
					"blank default at " + path + " — codegen would turn it into a wrong default value");
			node.properties().forEach(child -> assertNoBlankDefault(child.getValue(), path + "/" + child.getKey()));
		} else if (node.isArray()) {
			for (int i = 0; i < node.size(); i++) {
				assertNoBlankDefault(node.get(i), path + "/" + i);
			}
		}
	}

	private JsonNode liveSpec() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		return stripEphemeralFields(objectMapper.readTree(given().contentType(ContentType.JSON).when()
				.get("/api-docs").then().extract().asString()));
	}

	private JsonNode loadSnapshot() throws IOException {
		try (InputStream in = getClass().getResourceAsStream(SNAPSHOT_RESOURCE)) {
			if (in == null) {
				fail("No committed snapshot at src/test/resources" + SNAPSHOT_RESOURCE
						+ " — generate one with -DupdateOpenApiSnapshot=true");
			}
			String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return objectMapper.readTree(json);
		}
	}

	/**
	 * {@code servers[0].url} embeds {@code @LocalServerPort}'s random port — real API-shape drift,
	 * not this field, is what the test exists to catch, so it's removed from both sides before
	 * comparison (and before writing a fresh baseline) rather than compared verbatim.
	 */
	private JsonNode stripEphemeralFields(JsonNode spec) {
		if (spec instanceof tools.jackson.databind.node.ObjectNode objectNode) {
			objectNode.remove("servers");
		}
		return spec;
	}
}
