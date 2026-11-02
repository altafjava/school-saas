package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.application.service.FeatureFlagService;
import com.altafjava.school.application.security.SchoolFeatures;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import io.restassured.RestAssured;

/** A plan decides which modules a school has, whatever permissions its users hold. */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeatureEntitlementE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private FeatureFlagService featureFlagService;

	private School school;

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Entitlement School");
		SchoolFeatures.CATALOG.forEach(
				feature -> featureFlagService.setTenantOverride(school.tenantId(), feature.key(), false, "test"));
	}

	@ParameterizedTest
	@CsvSource({ "/api/v1/payslips", "/api/v1/salary-structures", "/api/v1/pay-component-definitions",
			"/api/v1/certificate-types", "/api/v1/hostel-buildings", "/api/v1/rooms", "/api/v1/medical-incidents" })
	void moduleOutsideThePlan_isRefused(String path) {
		support.request(school).get(path).then().statusCode(HttpStatus.FORBIDDEN.value())
				.body("error.code", equalTo("FEATURE_NOT_ENABLED"));
	}

	@Test
	void bulkImportOutsideThePlan_isRefused() {
		support.request(school).contentType("multipart/form-data").multiPart("file", "students.csv", new byte[] { 1 })
				.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
				.post("/api/v1/students/bulk-import").then().statusCode(HttpStatus.FORBIDDEN.value())
				.body("error.code", equalTo("FEATURE_NOT_ENABLED"));
	}

	@Test
	void moduleInsideThePlan_isAvailable() {
		featureFlagService.setTenantOverride(school.tenantId(), SchoolFeatures.PAYROLL, true, "test");

		support.request(school).get("/api/v1/payslips").then().statusCode(HttpStatus.OK.value());

		featureFlagService.setTenantOverride(school.tenantId(), SchoolFeatures.PAYROLL, false, "test");
	}

	@Test
	void coreModules_areNotPlanGated() {
		support.request(school).get("/api/v1/students").then().statusCode(HttpStatus.OK.value());
	}
}
