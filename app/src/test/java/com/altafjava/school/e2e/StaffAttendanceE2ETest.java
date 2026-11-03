package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
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
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/** Staff check in and out themselves; HR records the rest and reads the register, which shows approved leave. */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StaffAttendanceE2ETest extends SchoolIntegrationTestBase {

	private static final String BASE = "/api/v1/staff-attendance";

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private LeaveTypeRepository leaveTypeRepository;
	@Autowired
	private LeaveRequestRepository leaveRequestRepository;

	private School school;
	private Long teacherUser;
	private Long onLeaveUser;
	private String teacher;
	private String onLeave;
	private String absentee;
	private LocalDate today;

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Staff Attendance School");
		Long yearId = support.currentAcademicYearId(school);
		teacher = support.createEmployee(school, "Tara");
		onLeave = support.createEmployee(school, "Leo");
		absentee = support.createEmployee(school, "Abe");
		teacherUser = support.createUserWithRole(school.tenantId(), email("tara"), "TEACHER");
		onLeaveUser = support.createUserWithRole(school.tenantId(), email("leo"), "TEACHER");
		support.linkEmployeeToUser(school, teacher, teacherUser);
		support.linkEmployeeToUser(school, onLeave, onLeaveUser);
		// Tests provision no time zone, so the school's day is the server's.
		today = LocalDate.now();
		Long leaveEmployeeId = support.employeeId(school, onLeave);
		support.inTenant(school.tenantId(), () -> {
			Long typeId = leaveTypeRepository.save(LeaveType.create("Casual " + UUID.randomUUID(), BigDecimal.TEN))
					.getId();
			LeaveRequest leave = LeaveRequest.submit(leaveEmployeeId, typeId, yearId, today.minusDays(1),
					today.plusDays(1), "Family", BigDecimal.valueOf(3), 1);
			leave.approve(1L, today.atTime(9, 0));
			leaveRequestRepository.save(leave);
		});
	}

	@Test
	void checkInThenOut_recordsTheDayOnce() {
		staff(teacherUser).post(BASE + "/me/check-in").then().statusCode(HttpStatus.CREATED.value())
				.body("data.status", equalTo("PRESENT"))
				.body("data.date", equalTo(today.toString()))
				.body("data.employee.name", equalTo("Tara Staff"));
		staff(teacherUser).post(BASE + "/me/check-in").then().statusCode(HttpStatus.BAD_REQUEST.value());

		staff(teacherUser).post(BASE + "/me/check-out").then().statusCode(HttpStatus.OK.value())
				.body("data.checkOutAt", notNullValue());
		staff(teacherUser).post(BASE + "/me/check-out").then().statusCode(HttpStatus.BAD_REQUEST.value());
		staff(teacherUser).get(BASE + "/me").then().statusCode(HttpStatus.OK.value())
				.body("data.content.date", hasItem(today.toString()));
	}

	@Test
	void checkIn_onADayOfApprovedLeave_isRefused() {
		staff(onLeaveUser).post(BASE + "/me/check-in").then().statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void register_showsLeaveAndUnmarkedStaff() {
		admin().get(BASE + "/register?date=" + today + "&size=100").then().statusCode(HttpStatus.OK.value())
				.body("data.content.find { it.employeePublicId == '" + onLeave + "' }.status", equalTo("ON_LEAVE"))
				.body("data.content.find { it.employeePublicId == '" + onLeave + "' }.leaveRequestPublicId",
						notNullValue())
				.body("data.content.find { it.employeePublicId == '" + absentee + "' }.status",
						equalTo("NOT_MARKED"));
	}

	@Test
	void record_aShortDay_isAHalfDay_andAppearsOnTheRegister() {
		String day = today.minusDays(3).toString();
		admin().body("{\"status\":\"PRESENT\",\"checkInAt\":\"" + day + "T09:30:00\",\"checkOutAt\":\"" + day
				+ "T11:00:00\",\"note\":\"Doctor's visit\"}").put(BASE + "/employees/" + absentee + "/days/" + day)
				.then().statusCode(HttpStatus.OK.value())
				.body("data.status", equalTo("HALF_DAY"))
				.body("data.late", equalTo(true))
				.body("data.leftEarly", equalTo(true));
		admin().get(BASE + "/register?date=" + day + "&size=100").then()
				.body("data.content.find { it.employeePublicId == '" + absentee + "' }.status", equalTo("HALF_DAY"));
	}

	@Test
	void record_aDayNotYetCome_isRefused() {
		admin().body("{\"status\":\"ABSENT\"}")
				.put(BASE + "/employees/" + absentee + "/days/" + today.plusDays(2)).then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void record_aWorkingDayDuringApprovedLeave_isRefused() {
		admin().body("{\"status\":\"PRESENT\"}").put(BASE + "/employees/" + onLeave + "/days/" + today).then()
				.statusCode(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void workday_canBeChangedByHrAndIsValidated() {
		admin().body("{\"start\":\"08:00\",\"end\":\"16:00\",\"lateGraceMinutes\":5,\"halfDayMinutes\":200}")
				.put(BASE + "/workday").then().statusCode(HttpStatus.OK.value())
				.body("data.start", equalTo("08:00:00"));
		admin().body("{\"start\":\"16:00\",\"end\":\"08:00\",\"lateGraceMinutes\":5,\"halfDayMinutes\":200}")
				.put(BASE + "/workday").then().statusCode(HttpStatus.BAD_REQUEST.value());
		admin().body("{\"start\":\"09:00\",\"end\":\"17:00\",\"lateGraceMinutes\":10,\"halfDayMinutes\":240}")
				.put(BASE + "/workday").then().statusCode(HttpStatus.OK.value());
	}

	@Test
	void checkIn_withoutAStaffRecord_returns403() {
		Long stranger = support.createUserWithRole(school.tenantId(), email("nobody"), "TEACHER");

		staff(stranger).post(BASE + "/me/check-in").then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void register_asATeacher_returns403() {
		staff(teacherUser).get(BASE + "/register?date=" + today).then().statusCode(HttpStatus.FORBIDDEN.value());
		staff(teacherUser).body("{\"status\":\"ABSENT\"}").put(BASE + "/employees/" + absentee + "/days/" + today)
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void checkIn_asAParent_returns403() {
		support.requestWithRole(school.tenantId(), "PARENT").post(BASE + "/me/check-in").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void staffAttendance_withoutLogin_returns401() {
		given().header("X-Tenant-ID", school.tenantId()).get(BASE + "/register?date=" + today).then()
				.statusCode(HttpStatus.UNAUTHORIZED.value());
	}

	@Test
	void otherSchool_cannotSeeOrRecordThisSchoolsStaff() {
		School other = support.provisionSchool("Other Staff School");

		support.request(other).get(BASE + "/register?date=" + today + "&size=100").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.employeePublicId", org.hamcrest.Matchers.not(hasItem(teacher)));
		support.request(other).body("{\"status\":\"ABSENT\"}")
				.put(BASE + "/employees/" + absentee + "/days/" + today).then()
				.statusCode(HttpStatus.NOT_FOUND.value());
		support.requestAsUser(other.tenantId(), teacherUser, "TEACHER").post(BASE + "/me/check-in").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	private RequestSpecification admin() {
		return support.request(school);
	}

	private RequestSpecification staff(Long userId) {
		return support.requestAsUser(school.tenantId(), userId, "TEACHER");
	}

	private static String email(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@school.test";
	}
}
