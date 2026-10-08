package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.PeriodService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import io.restassured.RestAssured;

@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimetableVenueAndSubstitutionE2ETest extends SchoolIntegrationTestBase {

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private PeriodService periodService;
	@Autowired
	private ClassroomService classroomService;
	@Autowired
	private SubjectService subjectService;
	@Autowired
	private TeacherService teacherService;

	private School school;
	private Long periodId;
	private Long otherPeriodId;
	private Long classAId;
	private Long classBId;
	private Long subjectId;
	private Teacher regularTeacher;
	private Teacher busyTeacher;
	private Teacher freeTeacher;
	private String entryPublicId;
	private LocalDate nextMonday;

	@BeforeAll
	void setUp() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Timetable School");
		LocalDate today = LocalDate.now();
		nextMonday = today.plusDays((DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7 + 7);

		String academicYearPublicId = support.currentAcademicYearPublicId(school);
		support.inTenant(school.tenantId(), () -> {
			periodId = periodService.create("P1", LocalTime.of(9, 0), LocalTime.of(9, 45), 1).getId();
			otherPeriodId = periodService.create("P2", LocalTime.of(10, 0), LocalTime.of(10, 45), 2).getId();
			classAId = classroomService.create("TT-A", "Grade 5", "A", academicYearPublicId, null).getId();
			classBId = classroomService.create("TT-B", "Grade 5", "B", academicYearPublicId, null).getId();
			subjectId = subjectService.create("TT-MATH", "Mathematics", null).getId();
			regularTeacher = teacherService.hire("TT-T1", "Reg", "Teacher", "reg@school.test",
					LocalDate.of(2020, 1, 1));
			busyTeacher = teacherService.hire("TT-T2", "Busy", "Teacher", "busy@school.test", LocalDate.of(2020, 1, 1));
			freeTeacher = teacherService.hire("TT-T3", "Free", "Teacher", "free@school.test", LocalDate.of(2020, 1, 1));
		});

		entryPublicId = scheduleSlot(classAId, regularTeacher.getId(), periodId, null);
		scheduleSlot(classBId, busyTeacher.getId(), periodId, null);
	}

	private String scheduleSlot(Long classroomId, Long teacherId, Long forPeriodId, String venuePublicId) {
		String venue = venuePublicId == null ? "" : ",\"venuePublicId\":\"" + venuePublicId + "\"";
		return support.request(school)
				.body("{\"dayOfWeek\":\"MONDAY\",\"periodId\":" + forPeriodId + ",\"classroomId\":" + classroomId
						+ ",\"subjectId\":" + subjectId + ",\"teacherId\":" + teacherId + venue + "}")
				.post("/api/v1/timetable-entries")
				.then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	private String createVenue(String code) {
		return support.request(school)
				.body("{\"code\":\"" + code + "\",\"name\":\"" + code + "\",\"venueType\":\"LABORATORY\","
						+ "\"capacity\":30}")
				.post("/api/v1/venues")
				.then().statusCode(HttpStatus.CREATED.value())
				.body("data.active", equalTo(true))
				.extract().path("data.publicId");
	}

	@Test
	void venue_canBeCreatedOnceListedAndDeactivated() {
		String venuePublicId = createVenue("LAB-CRUD");

		support.request(school).body("{\"code\":\"LAB-CRUD\",\"name\":\"Again\",\"venueType\":\"HALL\"}")
				.post("/api/v1/venues").then().statusCode(HttpStatus.BAD_REQUEST.value());
		support.request(school).get("/api/v1/venues").then().statusCode(HttpStatus.OK.value())
				.body("data.content.publicId", hasItem(venuePublicId));
		support.request(school).patch("/api/v1/venues/" + venuePublicId + "/deactivate")
				.then().statusCode(HttpStatus.OK.value()).body("data.active", equalTo(false));
	}

	@Test
	void timetable_refusesToBookOneVenueTwiceInAPeriod() {
		String venuePublicId = createVenue("LAB-CLASH");
		String firstSlot = scheduleSlot(classAId, freeTeacher.getId(), otherPeriodId, venuePublicId);

		support.request(school)
				.body("{\"dayOfWeek\":\"MONDAY\",\"periodId\":" + otherPeriodId + ",\"classroomId\":" + classBId
						+ ",\"subjectId\":" + subjectId + ",\"teacherId\":" + busyTeacher.getId()
						+ ",\"venuePublicId\":\"" + venuePublicId + "\"}")
				.post("/api/v1/timetable-entries")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());

		support.request(school).body("{\"venuePublicId\":\"" + venuePublicId + "\"}")
				.patch("/api/v1/timetable-entries/" + firstSlot + "/venue")
				.then().statusCode(HttpStatus.OK.value());
	}

	@Test
	void substitution_assignmentLifecycle() {
		support.request(school).get("/api/v1/timetable-substitutions/available-teachers?timetableEntryPublicId="
				+ entryPublicId + "&date=" + nextMonday)
				.then().statusCode(HttpStatus.OK.value())
				.body("data.name", hasItem("Free Teacher"))
				.body("data.name", not(hasItem("Busy Teacher")))
				.body("data.name", not(hasItem("Reg Teacher")));

		String substitutionPublicId = support.request(school)
				.body("{\"timetableEntryPublicId\":\"" + entryPublicId + "\",\"date\":\"" + nextMonday
						+ "\",\"substituteTeacherPublicId\":\"" + freeTeacher.getPublicId() + "\",\"reason\":\"Sick\"}")
				.post("/api/v1/timetable-substitutions")
				.then().statusCode(HttpStatus.CREATED.value())
				.body("data.substituteTeacherName", equalTo("Free Teacher"))
				.body("data.regularTeacherName", equalTo("Reg Teacher"))
				.body("data.active", equalTo(true))
				.extract().path("data.publicId");

		support.request(school)
				.body("{\"timetableEntryPublicId\":\"" + entryPublicId + "\",\"date\":\"" + nextMonday
						+ "\",\"substituteTeacherPublicId\":\"" + freeTeacher.getPublicId() + "\"}")
				.post("/api/v1/timetable-substitutions")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
		support.request(school)
				.body("{\"timetableEntryPublicId\":\"" + entryPublicId + "\",\"date\":\"" + nextMonday
						+ "\",\"substituteTeacherPublicId\":\"" + busyTeacher.getPublicId() + "\"}")
				.post("/api/v1/timetable-substitutions")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());
		support.request(school)
				.body("{\"timetableEntryPublicId\":\"" + entryPublicId + "\",\"date\":\"" + nextMonday.plusDays(1)
						+ "\",\"substituteTeacherPublicId\":\"" + freeTeacher.getPublicId() + "\"}")
				.post("/api/v1/timetable-substitutions")
				.then().statusCode(HttpStatus.BAD_REQUEST.value());

		support.request(school).get("/api/v1/timetable-substitutions?date=" + nextMonday)
				.then().statusCode(HttpStatus.OK.value()).body("data", hasSize(1));

		support.request(school).body("{\"reason\":\"Teacher returned\"}")
				.patch("/api/v1/timetable-substitutions/" + substitutionPublicId + "/cancel")
				.then().statusCode(HttpStatus.OK.value()).body("data.active", equalTo(false));
		support.request(school).get("/api/v1/timetable-substitutions?date=" + nextMonday)
				.then().statusCode(HttpStatus.OK.value()).body("data", hasSize(0));
		support.request(school)
				.body("{\"timetableEntryPublicId\":\"" + entryPublicId + "\",\"date\":\"" + nextMonday
						+ "\",\"substituteTeacherPublicId\":\"" + freeTeacher.getPublicId() + "\"}")
				.post("/api/v1/timetable-substitutions")
				.then().statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void substitution_isNotReachableFromAnotherTenant() {
		School other = support.provisionSchool("Other Timetable School");

		support.request(other).body("{\"reason\":\"x\"}")
				.patch("/api/v1/timetable-substitutions/" + UUID.randomUUID() + "/cancel")
				.then().statusCode(HttpStatus.NOT_FOUND.value());
		support.request(other).get("/api/v1/timetable-substitutions?date=" + nextMonday)
				.then().statusCode(HttpStatus.OK.value()).body("data", hasSize(0));
	}

	@Test
	void assign_asTeacherRole_returns403() {
		support.requestWithRole(school.tenantId(), "TEACHER")
				.body("{\"timetableEntryPublicId\":\"" + entryPublicId + "\",\"date\":\"" + nextMonday
						+ "\",\"substituteTeacherPublicId\":\"" + freeTeacher.getPublicId() + "\"}")
				.post("/api/v1/timetable-substitutions")
				.then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void assign_withoutJwt_returns401() {
		RestAssured.given().header("X-Tenant-ID", school.tenantId())
				.contentType(io.restassured.http.ContentType.JSON)
				.body("{}")
				.post("/api/v1/timetable-substitutions")
				.then().statusCode(HttpStatus.UNAUTHORIZED.value());
	}
}
