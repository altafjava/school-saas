package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.school.application.service.LeaveBalanceService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import io.restassured.RestAssured;

// A leave type with two approval levels: the requester's department head first, then an administrator.
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeaveMultiLevelApprovalE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private LeaveBalanceService leaveBalanceService;
	@Autowired
	private LeaveTypeRepository leaveTypeRepository;

	private School school;
	private String leaveTypePublicId;
	private Long requesterUserId;
	private Long headUserId;

	@BeforeAll
	void setUp() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Leave School");

		String departmentPublicId = support.request(school)
				.body("{\"name\":\"Science\",\"code\":\"SCI\"}")
				.post("/api/v1/departments")
				.then().statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");

		String headPublicId = support.createEmployee(school, "Hal");
		headUserId = support.createUserWithRole(school.tenantId(), "head-" + UUID.randomUUID() + "@school.test",
				"TEACHER");
		support.linkEmployeeToUser(school, headPublicId, headUserId);
		support.request(school).body("{\"headEmployeePublicId\":\"" + headPublicId + "\"}")
				.patch("/api/v1/departments/" + departmentPublicId + "/head-employee")
				.then().statusCode(HttpStatus.OK.value());

		String requesterPublicId = support.createEmployee(school, "Rita");
		requesterUserId = support.createUserWithRole(school.tenantId(), "req-" + UUID.randomUUID() + "@school.test",
				"TEACHER");
		support.linkEmployeeToUser(school, requesterPublicId, requesterUserId);
		support.request(school).body("{\"departmentPublicId\":\"" + departmentPublicId + "\"}")
				.patch("/api/v1/employees/" + requesterPublicId + "/hr-details")
				.then().statusCode(HttpStatus.OK.value());

		leaveTypePublicId = support.request(school).body("{\"name\":\"Casual\",\"defaultAnnualDays\":10}")
				.post("/api/v1/leave-types")
				.then().statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
		support.request(school).body("{\"approvalLevels\":2}")
				.patch("/api/v1/leave-types/" + leaveTypePublicId + "/approval-levels")
				.then().statusCode(HttpStatus.OK.value()).body("data.approvalLevels", equalTo(2));

		Long leaveTypeId = support.inTenant(school.tenantId(), () -> leaveTypeRepository
				.findByPublicIdAndTenantId(UUID.fromString(leaveTypePublicId), school.tenantId()).orElseThrow()
				.getId());
		Long academicYearId = support.currentAcademicYearId(school);
		Long requesterId = support.employeeId(school, requesterPublicId);
		support.inTenant(school.tenantId(), () -> leaveBalanceService.allocateIfAbsent(requesterId, leaveTypeId,
				academicYearId, BigDecimal.TEN));
	}

	private String submitLeave(int fromDays, int toDays) {
		return support.requestAsUser(school.tenantId(), requesterUserId, "TEACHER")
				.body("{\"leaveTypePublicId\":\"" + leaveTypePublicId + "\",\"startDate\":\""
						+ LocalDate.now().plusDays(fromDays) + "\",\"endDate\":\"" + LocalDate.now().plusDays(toDays)
						+ "\",\"reason\":\"Family\"}")
				.post("/api/v1/leave-requests")
				.then().statusCode(HttpStatus.CREATED.value())
				.body("data.approvalsRequired", equalTo(2))
				.body("data.awaitingStage", equalTo("DEPARTMENT_HEAD"))
				.extract().path("data.publicId");
	}

	@Test
	void approvalFlow_needsTheDepartmentHeadThenAnAdministrator() {
		String requestPublicId = submitLeave(30, 31);

		support.request(school).patch("/api/v1/leave-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
		support.requestAsUser(school.tenantId(), requesterUserId, "TEACHER")
				.patch("/api/v1/leave-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.FORBIDDEN.value());

		support.requestAsUser(school.tenantId(), headUserId, "TEACHER")
				.get("/api/v1/leave-requests/awaiting-review")
				.then().statusCode(HttpStatus.OK.value()).body("data.content.publicId", hasItem(requestPublicId));
		support.requestAsUser(school.tenantId(), headUserId, "TEACHER")
				.patch("/api/v1/leave-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("PENDING"))
				.body("data.approvalsGranted", equalTo(1))
				.body("data.awaitingStage", equalTo("ADMINISTRATOR"));

		support.requestAsUser(school.tenantId(), headUserId, "TEACHER")
				.patch("/api/v1/leave-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.FORBIDDEN.value());

		support.request(school).patch("/api/v1/leave-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("APPROVED"))
				.body("data.awaitingStage", nullValue());

		support.request(school).get("/api/v1/leave-requests/" + requestPublicId + "/approvals")
				.then().statusCode(HttpStatus.OK.value())
				.body("data", hasSize(2))
				.body("data[0].stage", equalTo("DEPARTMENT_HEAD"))
				.body("data[1].stage", equalTo("ADMINISTRATOR"));
	}

	@Test
	void rejectionByTheDepartmentHead_endsTheRequest() {
		String requestPublicId = submitLeave(60, 61);

		support.requestAsUser(school.tenantId(), headUserId, "TEACHER")
				.body("{\"rejectionReason\":\"Exam week\"}")
				.patch("/api/v1/leave-requests/" + requestPublicId + "/reject")
				.then().statusCode(HttpStatus.OK.value()).body("data.status", equalTo("REJECTED"));

		support.request(school).patch("/api/v1/leave-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void approve_asStudentRole_returns403() {
		String requestPublicId = submitLeave(90, 91);

		support.requestWithRole(school.tenantId(), "STUDENT")
				.patch("/api/v1/leave-requests/" + requestPublicId + "/approve")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void approve_withoutJwt_returns401() {
		RestAssured.given().header("X-Tenant-ID", school.tenantId())
				.patch("/api/v1/leave-requests/" + UUID.randomUUID() + "/approve")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void overlappingRequest_isRefused() {
		submitLeave(120, 122);

		support.requestAsUser(school.tenantId(), requesterUserId, "TEACHER")
				.body("{\"leaveTypePublicId\":\"" + leaveTypePublicId + "\",\"startDate\":\""
						+ LocalDate.now().plusDays(121) + "\",\"endDate\":\"" + LocalDate.now().plusDays(123)
						+ "\"}")
				.post("/api/v1/leave-requests")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
	}
}
