package com.altafjava.school.e2e;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Assumptions;
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
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamSlot;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeAssignmentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.leave.model.StudentLeaveRequest;
import com.altafjava.school.domain.leave.repository.StudentLeaveRequestRepository;
import com.altafjava.school.domain.lms.model.Assignment;
import com.altafjava.school.domain.lms.model.Submission;
import com.altafjava.school.domain.lms.repository.AssignmentRepository;
import com.altafjava.school.domain.lms.repository.SubmissionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * The three home screens over one school: a class teacher with a current and a promoted class, a
 * parent with two children and one custody-restricted child, and a student.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HomeScreensE2ETest extends SchoolIntegrationTestBase {

	// Includes the request's own authentication and tenant lookups.
	private static final int STATEMENT_BUDGET = 45;
	// Scheduler threads share the statistics counter, so two runs can differ by a statement or two.
	private static final int BACKGROUND_NOISE = 3;
	private static final String CARDLESS_ROLE = "HOME_ONLY_TEACHER";
	private static final String TEACHER_PATH = "/api/v1/me/home/teacher";
	private static final String PARENT_PATH = "/api/v1/me/home/parent";
	private static final String STUDENT_PATH = "/api/v1/me/home/student";

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private EntityManagerFactory entityManagerFactory;
	@Autowired
	private AcademicYearRepository academicYearRepository;
	@Autowired
	private TeacherRepository teacherRepository;
	@Autowired
	private ClassroomRepository classroomRepository;
	@Autowired
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Autowired
	private SubjectRepository subjectRepository;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private GuardianRepository guardianRepository;
	@Autowired
	private StudentGuardianLinkRepository studentGuardianLinkRepository;
	@Autowired
	private PeriodRepository periodRepository;
	@Autowired
	private TimetableEntryRepository timetableEntryRepository;
	@Autowired
	private ExamRepository examRepository;
	@Autowired
	private ExamTypeDefinitionRepository examTypeDefinitionRepository;
	@Autowired
	private GradeRepository gradeRepository;
	@Autowired
	private AssignmentRepository assignmentRepository;
	@Autowired
	private SubmissionRepository submissionRepository;
	@Autowired
	private AttendanceRepository attendanceRepository;
	@Autowired
	private StudentLeaveRequestRepository studentLeaveRequestRepository;
	@Autowired
	private FeeStructureRepository feeStructureRepository;
	@Autowired
	private FeeAssignmentRepository feeAssignmentRepository;

	private School school;
	private Long tenantId;
	private Long yearId;
	private Long teacherUser;
	private Long teacherId;
	private Long noRecordTeacherUser;
	private Long parentUser;
	private Long noRecordParentUser;
	private Long familyOfOneUser;
	private Long studentUser;
	private Long noRecordStudentUser;
	private Long cardlessUser;
	private Classroom homeroom;
	private Classroom markedHomeroom;
	private Classroom promotedClass;
	private Subject maths;
	private Student ada;
	private Student ben;
	private Student restricted;
	private Student stranger;
	private Long examTypeId;

	@BeforeAll
	void buildSchool() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Home Screens School");
		tenantId = school.tenantId();
		yearId = support.currentAcademicYearId(school);
		teacherUser = support.createUserWithRole(tenantId, email("teacher"), "TEACHER");
		noRecordTeacherUser = support.createUserWithRole(tenantId, email("no-teacher"), "TEACHER");
		parentUser = support.createUserWithRole(tenantId, email("parent"), "PARENT");
		noRecordParentUser = support.createUserWithRole(tenantId, email("no-parent"), "PARENT");
		familyOfOneUser = support.createUserWithRole(tenantId, email("one-child"), "PARENT");
		studentUser = support.createUserWithRole(tenantId, email("student"), "STUDENT");
		noRecordStudentUser = support.createUserWithRole(tenantId, email("no-student"), "STUDENT");
		cardlessUser = support.createUserWithRole(tenantId, email("cardless"), "TEACHER");
		support.inTenant(tenantId, this::seed);
	}

	private void seed() {
		var currentYear = academicYearRepository.findByIdAndTenantId(yearId, tenantId).orElseThrow();
		Long previousYearId = academicYearRepository.save(AcademicYear.create("Previous " + suffix(),
				currentYear.getStartDate().minusYears(1), currentYear.getStartDate().minusDays(1), false)).getId();
		teacherId = teacherRecord(teacherUser);
		teacherRecord(cardlessUser);

		maths = subjectRepository.save(Subject.create("MATH-" + suffix(), "Maths", null));
		homeroom = classroom("HOME-A", yearId, teacherId);
		markedHomeroom = classroom("HOME-B", yearId, teacherId);
		promotedClass = classroom("OLD-A", previousYearId, teacherId);
		Classroom otherClass = classroom("OTHER", yearId, null);

		ada = student("Ada", homeroom);
		ben = student("Ben", homeroom);
		restricted = student("Rae", otherClass);
		stranger = student("Sam", otherClass);
		ada.setUserId(studentUser);
		studentRepository.save(ada);

		Guardian guardian = guardianRepository
				.save(Guardian.create("Gia", "Family", email("guardian"), "+14155552671", parentUser));
		link(guardian, ada);
		link(guardian, ben);
		StudentGuardianLink custody = StudentGuardianLink.create(restricted.getId(), guardian.getId(),
				RelationshipType.FATHER, false);
		custody.restrictCustody("Court order");
		studentGuardianLinkRepository.save(custody);
		Guardian singleParent = guardianRepository
				.save(Guardian.create("Una", "Single", email("guardian-one"), "+14155552672", familyOfOneUser));
		link(singleParent, ada);

		seedTimetable();
		seedExams();
		seedHomework();
		seedLeaveAttendanceAndFees();
	}

	private void seedTimetable() {
		DayOfWeek today = LocalDate.now().getDayOfWeek();
		Long periodId = periodRepository.save(Period.create("P1", LocalTime.of(9, 0), LocalTime.of(9, 45), 1)).getId();
		timetableEntryRepository
				.save(TimetableEntry.create(today, periodId, homeroom.getId(), maths.getId(), teacherId, null));
		Long laterPeriodId = periodRepository.save(Period.create("P2", LocalTime.of(10, 0), LocalTime.of(10, 45), 2))
				.getId();
		timetableEntryRepository.save(
				TimetableEntry.create(today, laterPeriodId, promotedClass.getId(), maths.getId(), teacherId, null));
	}

	private void seedExams() {
		examTypeId = examTypeDefinitionRepository.findByCodeAndTenantId("MIDTERM", tenantId)
				.map(ExamTypeDefinition::getId).orElseThrow();
		LocalDateTime now = LocalDateTime.now();
		Exam nextWeek = exam("Next week test", homeroom, now.plusDays(7), 10);
		nextWeek.publishSchedule();
		examRepository.save(nextWeek);
		exam("Unannounced test", homeroom, now.plusDays(8), 10);
		Exam needsMarks = exam("Sat yesterday", homeroom, now.minusDays(1), 10);
		gradeRepository.save(Grade.create(ada.getId(), maths.getId(), needsMarks.getId(), BigDecimal.TEN, "B", "t"));
		exam("Promoted class exam", promotedClass, now.minusDays(1), 10);

		Exam published = exam("Published test", homeroom, now.minusDays(20), 10);
		gradeRepository
				.save(Grade.create(ada.getId(), maths.getId(), published.getId(), BigDecimal.valueOf(80), "A", "t"));
		gradeRepository
				.save(Grade.create(ben.getId(), maths.getId(), published.getId(), BigDecimal.valueOf(60), "C", "t"));
		published.complete();
		published.verifyMarks("checker");
		published.lockMarks("registrar");
		published.publishResults("principal");
		examRepository.save(published);

		Exam hidden = exam("Unpublished test", homeroom, now.minusDays(15), 10);
		gradeRepository.save(Grade.create(ada.getId(), maths.getId(), hidden.getId(), BigDecimal.ONE, "F", "t"));
	}

	private void seedHomework() {
		LocalDate today = LocalDate.now();
		Assignment open = assignmentRepository.save(Assignment.create(homeroom.getId(), maths.getId(), teacherId,
				"Fractions", null, null, today.plusDays(3), BigDecimal.TEN));
		Assignment handedIn = assignmentRepository.save(Assignment.create(homeroom.getId(), maths.getId(), teacherId,
				"Decimals", null, null, today.plusDays(1), BigDecimal.TEN));
		submissionRepository
				.save(Submission.submit(handedIn.getId(), ada.getId(), null, "done", false, LocalDateTime.now()));
		submissionRepository
				.save(Submission.submit(open.getId(), ben.getId(), null, "done", true, LocalDateTime.now()));
		Assignment oldClassWork = assignmentRepository.save(Assignment.create(promotedClass.getId(), maths.getId(),
				teacherId, "Old worksheet", null, null, today.minusYears(1), BigDecimal.TEN));
		submissionRepository.save(
				Submission.submit(oldClassWork.getId(), stranger.getId(), null, "old", false, LocalDateTime.now()));
	}

	private void seedLeaveAttendanceAndFees() {
		LocalDate today = LocalDate.now();
		studentLeaveRequestRepository.save(StudentLeaveRequest.submit(ben.getId(), homeroom.getId(), parentUser,
				today.plusDays(2), today.plusDays(3), "Family trip", null));
		attendanceRepository.save(Attendance.create(ada.getId(), homeroom.getId(), today.minusDays(1),
				AttendanceStatus.PRESENT, "t"));
		attendanceRepository.save(Attendance.create(stranger.getId(), markedHomeroom.getId(), today,
				AttendanceStatus.PRESENT, "t"));
		FeeStructure tuition = feeStructureRepository
				.save(FeeStructure.create("Tuition", BigDecimal.valueOf(500), FeeFrequency.ANNUAL, null));
		feeAssignmentRepository.save(FeeAssignment.forStudent(tuition.getId(), ada.getId()));
	}

	// ---- teacher ----

	@Test
	void teacherHome_loadsEveryCardInOneCall() {
		teacher().get(TEACHER_PATH).then()
				.statusCode(HttpStatus.OK.value())
				.header("Cache-Control", "no-store")
				.body("data.date", notNullValue())
				.body("data.assignmentsToGrade.total", equalTo(2))
				.body("data.assignmentsToGrade.items.title", contains("Decimals", "Fractions"))
				.body("data.assignmentsToGrade.items[0].classroom.code", equalTo(homeroom.getClassCode()))
				.body("data.assignmentsToGrade.items[0].subject.name", equalTo("Maths"))
				.body("data.assignmentsToGrade.items[0].ungradedCount", equalTo(1))
				.body("data.examsNeedingMarks.items.title", hasItem("Sat yesterday"))
				.body("data.studentLeaveToReview.total", equalTo(1))
				.body("data.studentLeaveToReview.items[0].student.name", equalTo("Ben Family"))
				.body("data.staffLeaveToReview.total", equalTo(0))
				.body("data.inbox.unreadNotifications", equalTo(0))
				.body("data.inbox.unreadAnnouncements", equalTo(0))
				.body("data.inbox.unreadMessages", equalTo(0));
	}

	@Test
	void teacherHome_countsTheMarksStillMissing() {
		teacher().get(TEACHER_PATH).then()
				.body("data.examsNeedingMarks.items.find { it.title == 'Sat yesterday' }.marksMissing", equalTo(1));
	}

	@Test
	void teacherHome_leavesOutPublishedExamsAndExamsStillToCome() {
		teacher().get(TEACHER_PATH).then()
				.body("data.examsNeedingMarks.items.title", not(hasItem("Published test")))
				.body("data.examsNeedingMarks.items.title", not(hasItem("Next week test")));
	}

	@Test
	void teacherHome_leavesOutLastYearsClasses() {
		teacher().get(TEACHER_PATH).then()
				.body("data.examsNeedingMarks.items.title", not(hasItem("Promoted class exam")))
				.body("data.assignmentsToGrade.items.title", not(hasItem("Old worksheet")))
				.body("data.attendanceToMark.code", not(hasItem(promotedClass.getClassCode())));
	}

	@Test
	void teacherHome_onASchoolDay_listsTodaysSlotAndUnmarkedRegisters() {
		Assumptions.assumeFalse(LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY);

		teacher().get(TEACHER_PATH).then()
				.body("data.schoolDay", equalTo(true))
				.body("data.timetable", hasSize(1))
				.body("data.timetable[0].classroom.code", equalTo(homeroom.getClassCode()))
				.body("data.timetable[0].coverage", equalTo("REGULAR"))
				.body("data.attendanceToMark.code", contains(homeroom.getClassCode()));
	}

	@Test
	void teacherHome_cardsFollowTheirOwnPermissions() {
		String roleId = support.request(school)
				.body("{\"name\":\"" + CARDLESS_ROLE + "\",\"description\":\"Home only\"}")
				.post("/api/v1/roles").then().statusCode(HttpStatus.CREATED.value()).extract().path("data.id");
		support.request(school).body("{\"code\":\"TEACHER_HOME_READ\"}")
				.post("/api/v1/roles/" + roleId + "/permissions")
				.then().statusCode(HttpStatus.OK.value());

		support.requestAsUser(tenantId, cardlessUser, CARDLESS_ROLE).get(TEACHER_PATH).then()
				.statusCode(HttpStatus.OK.value())
				.body("data.timetable", nullValue())
				.body("data.attendanceToMark", nullValue())
				.body("data.assignmentsToGrade", nullValue())
				.body("data.examsNeedingMarks", nullValue())
				.body("data.studentLeaveToReview", nullValue())
				.body("data.staffLeaveToReview", nullValue())
				.body("data.inbox", notNullValue());
	}

	// ---- parent ----

	@Test
	void parentHome_givesOneSummaryPerLinkedChild() {
		parent().get(PARENT_PATH).then()
				.statusCode(HttpStatus.OK.value())
				.header("Cache-Control", "no-store")
				.body("data.children.student.name", containsInAnyOrder("Ada Family", "Ben Family"));
	}

	@Test
	void parentHome_leavesOutCustodyRestrictedAndUnrelatedChildren() {
		parent().get(PARENT_PATH).then()
				.body("data.children.student.publicId", not(hasItem(restricted.getPublicId().toString())))
				.body("data.children.student.publicId", not(hasItem(stranger.getPublicId().toString())));
	}

	@Test
	void parentHome_fillsEachChildsCards() {
		parent().get(PARENT_PATH).then()
				.body(childCard("Ada Family", "classroom.code"), equalTo(homeroom.getClassCode()))
				.body(childCard("Ada Family", "fees.outstanding"), equalTo(500.00F))
				.body(childCard("Ada Family", "attendance.markedDays"), equalTo(1))
				.body(childCard("Ada Family", "attendance.percentage"), equalTo(100.00F))
				.body(childCard("Ada Family", "upcomingExams.title"), contains("Next week test"))
				.body(childCard("Ada Family", "upcomingExams.title"), not(hasItem("Unannounced test")))
				.body(childCard("Ada Family", "homework.title"), contains("Fractions"))
				.body(childCard("Ben Family", "homework.title"), contains("Decimals"))
				.body(childCard("Ben Family", "fees.outstanding"), equalTo(0))
				.body(childCard("Ben Family", "leaveRequests.reason"), contains("Family trip"))
				.body(childCard("Ada Family", "leaveRequests"), empty());
	}

	@Test
	void parentHome_showsOnlyPublishedResults() {
		parent().get(PARENT_PATH).then()
				.body(childCard("Ada Family", "recentResults.examTitle"), contains("Published test"))
				.body(childCard("Ada Family", "recentResults[0].gradeLetter"), equalTo("A"))
				.body(childCard("Ben Family", "recentResults[0].gradeLetter"), equalTo("C"));
	}

	@Test
	void parentHome_aChildWithNoClassroom_stillGetsASummary() {
		Long user = support.createUserWithRole(tenantId, email("unplaced"), "PARENT");
		support.inTenant(tenantId, () -> {
			Student unplaced = studentRepository.save(Student.create("STU-" + suffix(), "Noa", "Family", email("noa"),
					LocalDate.of(2015, 1, 1)));
			link(guardianRepository.save(Guardian.create("Ola", "Family", email("ola"), "+14155552673", user)),
					unplaced);
		});

		support.requestAsUser(tenantId, user, "PARENT").get(PARENT_PATH).then()
				.statusCode(HttpStatus.OK.value())
				.body("data.children[0].student.name", equalTo("Noa Family"))
				.body("data.children[0].classroom", nullValue())
				.body("data.children[0].upcomingExams", empty());
	}

	@Test
	void parentHome_runsTheSameQueriesHoweverManyChildren() {
		long oneChild = statementsFor(support.requestAsUser(tenantId, familyOfOneUser, "PARENT"), PARENT_PATH);
		long twoChildren = statementsFor(parent(), PARENT_PATH);

		assertThat(twoChildren).isLessThanOrEqualTo(oneChild + BACKGROUND_NOISE);
		assertThat(twoChildren).isLessThanOrEqualTo(STATEMENT_BUDGET);
	}

	@Test
	void teacherAndStudentHome_stayWithinTheStatementBudget() {
		assertThat(statementsFor(teacher(), TEACHER_PATH)).isLessThanOrEqualTo(STATEMENT_BUDGET);
		assertThat(statementsFor(student(), STUDENT_PATH)).isLessThanOrEqualTo(STATEMENT_BUDGET);
	}

	@Test
	void schoolProfile_leavesOutACustodyRestrictedChild() {
		parent().get("/api/v1/me/school-profile").then()
				.statusCode(HttpStatus.OK.value())
				.body("data.linkedStudents.name", containsInAnyOrder("Ada Family", "Ben Family"));
	}

	// ---- student ----

	@Test
	void studentHome_returnsTheirOwnCards() {
		student().get(STUDENT_PATH).then()
				.statusCode(HttpStatus.OK.value())
				.header("Cache-Control", "no-store")
				.body("data.overview.student.name", equalTo("Ada Family"))
				.body("data.overview.fees.outstanding", equalTo(500.00F))
				.body("data.overview.recentResults.examTitle", contains("Published test"))
				.body("data.overview.homework.title", contains("Fractions"))
				.body("data.overview.upcomingExams.title", contains("Next week test"))
				.body("data.overview.leaveRequests", nullValue())
				.body("data.inbox.unreadAnnouncements", equalTo(0))
				.body("data.inbox.unreadMessages", nullValue());
	}

	@Test
	void studentHome_onASchoolDay_listsTheClassroomsDay() {
		Assumptions.assumeFalse(LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY);

		student().get(STUDENT_PATH).then()
				.body("data.timetable", hasSize(1))
				.body("data.timetable[0].subject.name", equalTo("Maths"))
				.body("data.timetable[0].teacher.name", equalTo("Mira Teacher"));
	}

	// ---- access ----

	@Test
	void home_withoutLogin_returns401() {
		for (String path : List.of(TEACHER_PATH, PARENT_PATH, STUDENT_PATH)) {
			given().header("X-Tenant-ID", tenantId).get(path).then().statusCode(HttpStatus.UNAUTHORIZED.value());
		}
	}

	@Test
	void home_ofAnotherRole_returns403() {
		student().get(TEACHER_PATH).then().statusCode(HttpStatus.FORBIDDEN.value());
		student().get(PARENT_PATH).then().statusCode(HttpStatus.FORBIDDEN.value());
		parent().get(TEACHER_PATH).then().statusCode(HttpStatus.FORBIDDEN.value());
		parent().get(STUDENT_PATH).then().statusCode(HttpStatus.FORBIDDEN.value());
		teacher().get(PARENT_PATH).then().statusCode(HttpStatus.FORBIDDEN.value());
		teacher().get(STUDENT_PATH).then().statusCode(HttpStatus.FORBIDDEN.value());
		support.request(school).get(TEACHER_PATH).then().statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void home_withoutASchoolRecord_returns403() {
		support.requestAsUser(tenantId, noRecordTeacherUser, "TEACHER").get(TEACHER_PATH).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		support.requestAsUser(tenantId, noRecordParentUser, "PARENT").get(PARENT_PATH).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		support.requestAsUser(tenantId, noRecordStudentUser, "STUDENT").get(STUDENT_PATH).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void home_inAnotherTenant_findsNoRecordOfTheCaller() {
		School other = support.provisionSchool("Other Home School");

		support.requestAsUser(other.tenantId(), teacherUser, "TEACHER").get(TEACHER_PATH).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		support.requestAsUser(other.tenantId(), parentUser, "PARENT").get(PARENT_PATH).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
		support.requestAsUser(other.tenantId(), studentUser, "STUDENT").get(STUDENT_PATH).then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	private String childCard(String childName, String card) {
		return "data.children.find { it.student.name == '" + childName + "' }." + card;
	}

	// First call warms per-role caches, so the counted call sees only what the screen itself costs.
	private long statementsFor(RequestSpecification caller, String path) {
		caller.get(path).then().statusCode(HttpStatus.OK.value());
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		statistics.clear();
		caller.get(path).then().statusCode(HttpStatus.OK.value());
		return statistics.getPrepareStatementCount();
	}

	private RequestSpecification teacher() {
		return support.requestAsUser(tenantId, teacherUser, "TEACHER");
	}

	private RequestSpecification parent() {
		return support.requestAsUser(tenantId, parentUser, "PARENT");
	}

	private RequestSpecification student() {
		return support.requestAsUser(tenantId, studentUser, "STUDENT");
	}

	private Long teacherRecord(Long userId) {
		var teacher = Teacher.create("EMP-" + suffix(), "Mira", "Teacher",
				email("staff"), LocalDate.of(2020, 8, 1));
		teacher.setUserId(userId);
		return teacherRepository.save(teacher).getId();
	}

	private Classroom classroom(String prefix, Long academicYearId, Long classTeacherId) {
		return classroomRepository.save(Classroom.create(prefix + "-" + suffix(), "5", prefix, academicYearId, "AY",
				classTeacherId));
	}

	private Student student(String firstName, Classroom classroom) {
		Student student = studentRepository.save(Student.create("STU-" + suffix(), firstName, "Family",
				email(firstName.toLowerCase()), LocalDate.of(2015, 1, 1)));
		studentClassroomLinkRepository.save(StudentClassroomLink.create(student.getId(), classroom.getId(),
				classroom.getAcademicYearId(), LocalDate.now().minusDays(30)));
		return student;
	}

	private void link(Guardian guardian, Student student) {
		studentGuardianLinkRepository
				.save(StudentGuardianLink.create(student.getId(), guardian.getId(), RelationshipType.MOTHER, true));
	}

	private Exam exam(String title, Classroom classroom, LocalDateTime scheduledAt, int weightage) {
		return examRepository
				.save(Exam.create(title, maths.getId(), classroom.getId(), new ExamSlot(scheduledAt, 60, null),
						BigDecimal.valueOf(100), null, examTypeId, BigDecimal.valueOf(weightage)));
	}

	private static String suffix() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private static String email(String prefix) {
		return prefix + "-" + suffix() + "@school.test";
	}
}
