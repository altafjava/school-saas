package com.altafjava.school.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
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
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * The classroom-scope matrix. One school: classroom A has a class teacher and a timetabled maths
 * teacher, classroom B has neither. Each role must reach exactly its own slice of attendance,
 * grades, rosters and coursework, whatever permissions it holds.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AcademicScopeE2ETest extends SchoolIntegrationTestBase {

	private static final String AUDITOR_ROLE = "ACADEMIC_AUDITOR";

	@LocalServerPort
	int port;

	@Autowired
	private TestPublicIds publicIds;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private TeacherRepository teacherRepository;
	@Autowired
	private ClassroomRepository classroomRepository;
	@Autowired
	private SubjectRepository subjectRepository;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private ExamRepository examRepository;
	@Autowired
	private ExamTypeDefinitionRepository examTypeDefinitionRepository;
	@Autowired
	private PeriodRepository periodRepository;
	@Autowired
	private TimetableEntryRepository timetableEntryRepository;
	@Autowired
	private AcademicYearService academicYearService;

	private School school;
	private String classroomA;
	private String classroomB;
	private Long classroomAId;
	private Long classroomBId;
	private String mathPublicId;
	private String englishPublicId;
	private String studentA;
	private Long studentAId;
	private Long studentBId;
	private Long mathExamAId;
	private Long englishExamAId;
	private Long mathExamBId;
	private String attendanceOfA;
	private String attendanceOfB;

	private Long classTeacherUser;
	private Long mathTeacherUser;
	private Long unassignedTeacherUser;
	private Long parentOfAUser;
	private Long studentAUser;
	private Long auditorUser;

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Academic Scope School");

		classTeacherUser = teacherUser("class");
		mathTeacherUser = teacherUser("math");
		unassignedTeacherUser = teacherUser("idle");
		Long classTeacherId = teacherIdOf(classTeacherUser);
		Long mathTeacherId = teacherIdOf(mathTeacherUser);

		classroomA = support.createClassroom(school, "CLS-A");
		classroomB = support.createClassroom(school, "CLS-B");
		classroomAId = classroomId(classroomA);
		classroomBId = classroomId(classroomB);
		mathPublicId = support.createSubject(school, "MATH");
		englishPublicId = support.createSubject(school, "ENG");
		Long mathId = subjectId(mathPublicId);
		Long englishId = subjectId(englishPublicId);
		support.inTenant(school.tenantId(), () -> {
			var classroom = classroomRepository.findByIdAndTenantId(classroomAId, school.tenantId()).orElseThrow();
			classroom.reassignTeacher(classTeacherId);
			classroomRepository.save(classroom);
			Long periodId = periodRepository
					.save(Period.create("P1", LocalTime.of(9, 0), LocalTime.of(9, 45), 1)).getId();
			timetableEntryRepository.save(
					TimetableEntry.create(DayOfWeek.MONDAY, periodId, classroomAId, mathId, mathTeacherId, null));
		});

		studentA = support.createStudent(school);
		String studentB = support.createStudent(school);
		studentAId = studentId(studentA);
		studentBId = studentId(studentB);
		enroll(classroomA, studentA);
		enroll(classroomB, studentB);

		mathExamAId = examId(createExam("Maths A", mathId, classroomAId));
		englishExamAId = examId(createExam("English A", englishId, classroomAId));
		mathExamBId = examId(createExam("Maths B", mathId, classroomBId));
		recordGrade(admin(), studentAId, mathExamAId).then().statusCode(HttpStatus.CREATED.value());
		recordGrade(admin(), studentBId, mathExamBId).then().statusCode(HttpStatus.CREATED.value());
		attendanceOfA = markAttendance(admin(), studentAId, classroomAId, "2026-02-02").then()
				.statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");
		attendanceOfB = markAttendance(admin(), studentBId, classroomBId, "2026-02-02").then()
				.statusCode(HttpStatus.CREATED.value()).extract().path("data.publicId");

		parentOfAUser = support.createUserWithRole(school.tenantId(), email("parent"), "PARENT");
		String guardian = admin()
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"" + email("guardian")
						+ "\",\"phone\":\"+14155552671\",\"userPublicId\":\"" + publicIds.of(User.class, parentOfAUser)
						+ "\"}")
				.post("/api/v1/guardians").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
		admin().body("{\"studentPublicId\":\"" + studentA + "\",\"relationshipType\":\"MOTHER\","
				+ "\"primaryContact\":true}")
				.post("/api/v1/guardians/" + guardian + "/students").then().statusCode(HttpStatus.CREATED.value());

		studentAUser = support.createUserWithRole(school.tenantId(), email("student"), "STUDENT");
		support.inTenant(school.tenantId(), () -> {
			var student = studentRepository.findByIdAndTenantId(studentAId, school.tenantId()).orElseThrow();
			student.setUserId(studentAUser);
			studentRepository.save(student);
		});

		// Authorities come from the token, so the seeded role here only gets the user row created.
		auditorUser = support.createUserWithRole(school.tenantId(), email("auditor"), "STUDENT");
		String roleId = admin().body("{\"name\":\"" + AUDITOR_ROLE + "\",\"description\":\"Reads every classroom\"}")
				.post("/api/v1/roles").then().statusCode(HttpStatus.CREATED.value()).extract().path("data.id");
		for (String code : new String[] { "STUDENT_ATTENDANCE_READ", "STUDENT_ATTENDANCE_WRITE",
				"STUDENT_GRADES_READ", "ALL_CLASSROOMS_READ" }) {
			admin().body("{\"code\":\"" + code + "\"}").post("/api/v1/roles/" + roleId + "/permissions").then()
					.statusCode(HttpStatus.OK.value());
		}
	}

	// ---- attendance ----

	@Test
	void attendanceList_isNarrowedToEachCallersScope() {
		admin().get("/api/v1/attendance?size=100").then()
				.body("data.content.publicId", hasItems(attendanceOfA, attendanceOfB));
		as(auditorUser, AUDITOR_ROLE).get("/api/v1/attendance?size=100").then()
				.body("data.content.publicId", hasItems(attendanceOfA, attendanceOfB));
		for (Long teacher : new Long[] { classTeacherUser, mathTeacherUser }) {
			as(teacher, "TEACHER").get("/api/v1/attendance?size=100").then()
					.statusCode(HttpStatus.OK.value())
					.body("data.content.publicId", hasItem(attendanceOfA))
					.body("data.content.classroomPublicId", everyItem(equalTo(classroomA)));
		}
		as(unassignedTeacherUser, "TEACHER").get("/api/v1/attendance?size=100").then()
				.body("data.content", hasSize(0));
		for (RequestSpecification family : new RequestSpecification[] { as(parentOfAUser, "PARENT"),
				as(studentAUser, "STUDENT") }) {
			family.get("/api/v1/attendance?size=100").then()
					.statusCode(HttpStatus.OK.value())
					.body("data.content.publicId", hasItem(attendanceOfA))
					.body("data.content.studentPublicId", everyItem(equalTo(studentA)));
		}
	}

	@Test
	void attendanceRecord_ofAnotherStudent_isForbiddenToParentAndStudent() {
		as(parentOfAUser, "PARENT").get("/api/v1/attendance/" + attendanceOfA).then()
				.statusCode(HttpStatus.OK.value());
		as(parentOfAUser, "PARENT").get("/api/v1/attendance/" + attendanceOfB).then()
				.statusCode(HttpStatus.FORBIDDEN.value())
				.body("error.code", equalTo("FORBIDDEN"));
		as(studentAUser, "STUDENT").get("/api/v1/attendance/" + attendanceOfB).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		as(studentAUser, "STUDENT").get("/api/v1/attendance/" + attendanceOfB + "/corrections").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void attendanceMarking_isLimitedToTeachersOfTheClassroom() {
		markAttendance(as(classTeacherUser, "TEACHER"), studentAId, classroomAId, "2026-02-03").then()
				.statusCode(HttpStatus.CREATED.value());
		markAttendance(as(mathTeacherUser, "TEACHER"), studentAId, classroomAId, "2026-02-04").then()
				.statusCode(HttpStatus.CREATED.value());
		markAttendance(as(classTeacherUser, "TEACHER"), studentBId, classroomBId, "2026-02-03").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		markAttendance(as(unassignedTeacherUser, "TEACHER"), studentAId, classroomAId, "2026-02-05").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		as(unassignedTeacherUser, "TEACHER").body("{\"status\":\"ABSENT\"}")
				.patch("/api/v1/attendance/" + attendanceOfA + "/status").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void readAllScope_doesNotGrantWritesOutsideTaughtClassrooms() {
		markAttendance(as(auditorUser, AUDITOR_ROLE), studentBId, classroomBId, "2026-02-06").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		markAttendance(admin(), studentBId, classroomBId, "2026-02-06").then()
				.statusCode(HttpStatus.CREATED.value());
	}

	// ---- grades ----

	@Test
	void gradeEntry_isLimitedToTheSubjectTaught_exceptForTheClassTeacher() {
		recordGrade(as(mathTeacherUser, "TEACHER"), studentAId, englishExamAId).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		recordGrade(as(unassignedTeacherUser, "TEACHER"), studentAId, englishExamAId).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		recordGrade(as(classTeacherUser, "TEACHER"), studentAId, englishExamAId).then()
				.statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void gradeList_isNarrowedToEachCallersScope() {
		as(mathTeacherUser, "TEACHER").get("/api/v1/grades?size=100").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content.examPublicId", hasItem(publicIds.of(Exam.class, mathExamAId)))
				.body("data.content.examPublicId", not(hasItem(publicIds.of(Exam.class, mathExamBId))))
				.body("data.content.examPublicId", not(hasItem(publicIds.of(Exam.class, englishExamAId))));
		as(unassignedTeacherUser, "TEACHER").get("/api/v1/grades?size=100").then()
				.body("data.content", hasSize(0));
		as(auditorUser, AUDITOR_ROLE).get("/api/v1/grades?size=100").then()
				.body("data.content.studentPublicId.unique()", hasSize(2));
	}

	@Test
	void parentAndStudent_seeOwnGradesOnlyOncePublished() {
		String mathExamC = createExam("Maths quiz", subjectId(mathPublicId), classroomAId);
		recordGrade(admin(), studentAId, examId(mathExamC)).then().statusCode(HttpStatus.CREATED.value());
		int visibleBefore = as(parentOfAUser, "PARENT").get("/api/v1/grades?size=100").then()
				.statusCode(HttpStatus.OK.value()).extract().path("data.content.size()");

		admin().patch("/api/v1/exams/" + mathExamC + "/complete").then().statusCode(HttpStatus.OK.value());
		admin().post("/api/v1/exams/" + mathExamC + "/results/publish").then().statusCode(HttpStatus.OK.value());

		as(parentOfAUser, "PARENT").get("/api/v1/grades?size=100").then()
				.body("data.content", hasSize(visibleBefore + 1))
				.body("data.content.studentPublicId", everyItem(equalTo(studentA)));
		as(studentAUser, "STUDENT").get("/api/v1/grades?size=100").then()
				.body("data.content.studentPublicId", everyItem(equalTo(studentA)));
	}

	// ---- roster and coursework ----

	@Test
	void roster_isHiddenFromStudentsAndParents() {
		as(studentAUser, "STUDENT").get("/api/v1/classrooms/" + classroomA + "/students").then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		as(classTeacherUser, "TEACHER").get("/api/v1/classrooms/" + classroomA + "/students").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content", hasSize(1));
	}

	@Test
	void coursework_isAuthoredBySubjectTeachers_andReadByTheClassroomsOwnStudents() {
		createAssignment(as(mathTeacherUser, "TEACHER"), classroomA, englishPublicId).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		createAssignment(as(mathTeacherUser, "TEACHER"), classroomB, mathPublicId).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		createAssignment(as(mathTeacherUser, "TEACHER"), classroomA, mathPublicId).then()
				.statusCode(HttpStatus.CREATED.value());

		as(studentAUser, "STUDENT").get("/api/v1/assignments/classroom/" + classroomA).then()
				.statusCode(HttpStatus.OK.value())
				.body("data.content", hasSize(1));
		as(parentOfAUser, "PARENT").get("/api/v1/assignments/classroom/" + classroomA).then()
				.statusCode(HttpStatus.OK.value());
		as(studentAUser, "STUDENT").get("/api/v1/assignments/classroom/" + classroomB).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		as(parentOfAUser, "PARENT").get("/api/v1/assignments/classroom/" + classroomB).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		as(unassignedTeacherUser, "TEACHER").get("/api/v1/assignments/classroom/" + classroomA).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	// ---- fixture helpers ----

	private RequestSpecification admin() {
		return support.request(school);
	}

	private RequestSpecification as(Long userId, String role) {
		return support.requestAsUser(school.tenantId(), userId, role);
	}

	private String email(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@school.test";
	}

	private Long teacherUser(String label) {
		Long userId = support.createUserWithRole(school.tenantId(), email("teacher-" + label), "TEACHER");
		String code = "EMP-" + label + "-" + UUID.randomUUID().toString().substring(0, 6);
		String teacherPublicId = admin()
				.body("{\"employeeCode\":\"" + code + "\",\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\""
						+ code + "@school.test\",\"joinDate\":\"2020-08-01\"}")
				.post("/api/v1/teachers").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
		support.inTenant(school.tenantId(), () -> {
			var teacher = teacherRepository
					.findByPublicIdAndTenantId(UUID.fromString(teacherPublicId), school.tenantId()).orElseThrow();
			teacher.setUserId(userId);
			teacherRepository.save(teacher);
		});
		return userId;
	}

	private Long teacherIdOf(Long userId) {
		return support.inTenant(school.tenantId(),
				() -> teacherRepository.findByUserIdAndTenantId(userId, school.tenantId()).orElseThrow().getId());
	}

	private Long classroomId(String publicId) {
		return support.inTenant(school.tenantId(), () -> classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), school.tenantId()).orElseThrow().getId());
	}

	private Long subjectId(String publicId) {
		return support.inTenant(school.tenantId(), () -> subjectRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), school.tenantId()).orElseThrow().getId());
	}

	private Long studentId(String publicId) {
		return support.inTenant(school.tenantId(), () -> studentRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), school.tenantId()).orElseThrow().getId());
	}

	private Long examId(String publicId) {
		return support.inTenant(school.tenantId(), () -> examRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), school.tenantId()).orElseThrow().getId());
	}

	private void enroll(String classroomPublicId, String studentPublicId) {
		String academicYear = support.inTenant(school.tenantId(), () -> academicYearService
				.create("enroll-" + UUID.randomUUID().toString().substring(0, 8), LocalDate.of(2025, 6, 1),
						LocalDate.of(2026, 5, 31), true)
				.getPublicId().toString());
		admin().body("{\"studentPublicId\":\"" + studentPublicId + "\",\"academicYearPublicId\":\"" + academicYear
				+ "\"}").post("/api/v1/classrooms/" + classroomPublicId + "/students").then()
				.statusCode(HttpStatus.CREATED.value());
	}

	private String createExam(String title, Long subjectId, Long classroomId) {
		Long examTypeId = support.inTenant(school.tenantId(), () -> examTypeDefinitionRepository
				.findByCodeAndTenantId("MIDTERM", school.tenantId()).orElseThrow().getId());
		return admin()
				.body("{\"title\":\"" + title + "\",\"subjectPublicId\":\"" + publicIds.of(Subject.class, subjectId)
						+ "\",\"classroomPublicId\":\"" + publicIds.of(Classroom.class, classroomId)
						+ "\",\"scheduledAt\":\"2026-03-01T09:00:00\",\"maxMarks\":100,\"examTypePublicId\":\""
						+ publicIds.of(ExamTypeDefinition.class, examTypeId)
						+ "\",\"weightage\":10}")
				.post("/api/v1/exams").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
	}

	private Response recordGrade(RequestSpecification caller, Long studentId, Long examId) {
		return caller.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId) + "\",\"examPublicId\":\""
				+ publicIds.of(Exam.class, examId) + "\",\"marks\":85}").post("/api/v1/grades");
	}

	private Response markAttendance(RequestSpecification caller, Long studentId,
			Long classroomId, String date) {
		return caller.header("Idempotency-Key", UUID.randomUUID().toString())
				.body("{\"studentPublicId\":\"" + publicIds.of(Student.class, studentId) + "\",\"classroomPublicId\":\""
						+ publicIds.of(Classroom.class, classroomId) + "\",\"attendanceDate\":\""
						+ date + "\",\"status\":\"PRESENT\"}")
				.post("/api/v1/attendance");
	}

	private Response createAssignment(RequestSpecification caller, String classroomPublicId,
			String subjectPublicId) {
		return caller.header("Idempotency-Key", UUID.randomUUID().toString())
				.body("{\"classroomPublicId\":\"" + classroomPublicId + "\",\"subjectPublicId\":\"" + subjectPublicId
						+ "\",\"title\":\"Worksheet\",\"dueDate\":\"2026-12-01\",\"maxMarks\":10}")
				.post("/api/v1/assignments");
	}
}
