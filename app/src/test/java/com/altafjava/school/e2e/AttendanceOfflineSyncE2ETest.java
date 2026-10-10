package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.util.SchoolAuthenticationHelper;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Proves {@code AttendanceOfflineSyncHandler} is genuinely wired end to end: a device syncs an
 * offline-marked attendance record through the generic {@code /api/v1/sync/**} pipeline, and the
 * result is independently visible through the ordinary {@code AttendanceController} read path —
 * not just internally self-consistent within the sync machinery.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttendanceOfflineSyncE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private TestPublicIds publicIds;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private SchoolAuthenticationHelper authHelper;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private StudentService studentService;

	private Long tenantId;
	private String adminEmail;
	private String adminPassword;
	private String academicYearPublicId;
	private String classroomPublicId;
	private String studentPublicId;

	@BeforeEach
	void setup() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		adminEmail = "sync-admin-" + suffix + "@school.test";
		adminPassword = "Password123!";
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Attendance Sync School", "att-sync-" + suffix, 1L, adminEmail, adminPassword, "USD"));
		tenantId = tenant.getId();

		TenantContext.ForTesting.setCurrentTenant(tenantId, tenant.getPublicId(), tenant.getSubdomain(),
				TenantType.SHARED);
		try {
			academicYearPublicId = academicYearService
					.create("2025-26", LocalDate.of(2025, 6, 1), LocalDate.of(2026, 5, 31), true)
					.getPublicId().toString();
			Classroom classroom = classroomService.create("CLS-SYNC-1", "Grade 5", "A", academicYearPublicId, null);
			classroomPublicId = classroom.getPublicId().toString();
			Student student = studentService.enroll("STU-SYNC-1", "Alice", "Smith", "alice@sync.test",
					LocalDate.of(2010, 1, 1));
			studentPublicId = student.getPublicId().toString();
			classroomService.enrollStudent(classroomPublicId, studentPublicId, academicYearPublicId);
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private String login() {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			var response = given()
					.header("X-Tenant-ID", tenantId)
					.contentType(ContentType.JSON)
					.body("{\"email\":\"" + adminEmail + "\",\"password\":\"" + adminPassword + "\"}")
					.post("/api/v1/auth/login");
			if (response.statusCode() == HttpStatus.OK.value()) {
				return response.then().extract().path("data.accessToken");
			}
			if (System.currentTimeMillis() >= deadline) {
				response.then().statusCode(HttpStatus.OK.value());
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		throw new IllegalStateException("login timed out");
	}

	@Test
	void offlineMarkedAttendance_syncsAndIsVisibleThroughTheNormalAttendanceReadPath() {
		String accessToken = login();
		String payload = "{\"studentPublicId\":\"" + studentPublicId + "\",\"classroomPublicId\":\""
				+ classroomPublicId + "\",\"attendanceDate\":\"2026-01-15\",\"status\":\"PRESENT\"}";

		String entry = """
				{
				  "operationType": "CREATE",
				  "entityType": "attendance",
				  "payloadJson": %s,
				  "clientTimestamp": "%s",
				  "clientGeneratedId": "%s"
				}
				""".formatted(
				"\"" + payload.replace("\"", "\\\"") + "\"",
				java.time.Instant.now(),
				UUID.randomUUID());

		String serverEntityId = given()
				.header("Authorization", "Bearer " + accessToken)
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("{\"deviceId\": \"device-offline-1\", \"entries\": [" + entry + "]}")
				.when()
				.post("/api/v1/sync/queue")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data[0].syncStatus", equalTo("SYNCED"))
				.body("data[0].serverEntityId", notNullValue())
				.extract().path("data[0].serverEntityId");

		// Proves the sync pipeline really applied AttendanceService.mark — not just recorded a
		// queue-row status flip — by reading it back through the ordinary REST read path.
		given()
				.header("Authorization", "Bearer " + accessToken)
				.header("X-Tenant-ID", tenantId)
				.when()
				.get("/api/v1/attendance/" + serverEntityId)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("PRESENT"))
				.body("data.markedBy", equalTo(adminEmail));
	}

	@Test
	void offlineMarkedAttendance_forStudentNotOnRoster_isRejectedByRealBusinessRule() {
		String accessToken = login();
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		String unenrolledStudentPublicId;
		try {
			unenrolledStudentPublicId = studentService
					.enroll("STU-SYNC-2", "Bob", "Jones", "bob@sync.test", LocalDate.of(2011, 2, 2))
					.getPublicId().toString();
		} finally {
			TenantContext.ForTesting.clear();
		}

		String payload = "{\"studentPublicId\":\"" + unenrolledStudentPublicId + "\",\"classroomPublicId\":\""
				+ classroomPublicId + "\",\"attendanceDate\":\"2026-01-15\",\"status\":\"PRESENT\"}";
		String entry = """
				{
				  "operationType": "CREATE",
				  "entityType": "attendance",
				  "payloadJson": %s,
				  "clientTimestamp": "%s",
				  "clientGeneratedId": "%s"
				}
				""".formatted(
				"\"" + payload.replace("\"", "\\\"") + "\"",
				java.time.Instant.now(),
				UUID.randomUUID());

		// AttendanceService.mark's roster-membership guard must still run for a synced write, the
		// same as it would for a direct online request — the sync path is not a bypass.
		given()
				.header("Authorization", "Bearer " + accessToken)
				.header("X-Tenant-ID", tenantId)
				.contentType(ContentType.JSON)
				.body("{\"deviceId\": \"device-offline-2\", \"entries\": [" + entry + "]}")
				.when()
				.post("/api/v1/sync/queue")
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("data[0].syncStatus", equalTo("FAILED"));
	}
}
