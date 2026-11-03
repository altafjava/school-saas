package com.altafjava.school.e2e;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import com.altafjava.platform.application.service.NotificationPreferenceService;
import com.altafjava.platform.domain.notification.model.NotificationChannel;
import com.altafjava.platform.domain.notification.model.NotificationTypes;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.util.SchoolE2eSupport;
import com.altafjava.school.util.SchoolE2eSupport.School;
import com.altafjava.school.util.TestPublicIds;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/** Announcements, emergency alerts and guardian-teacher messaging, checked against the phase's acceptance list. */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommunicationE2ETest extends SchoolIntegrationTestBase {

	private static final String ANNOUNCEMENTS = "/api/v1/announcements";
	private static final String CONVERSATIONS = "/api/v1/conversations";

	@LocalServerPort
	int port;

	@Autowired
	private SchoolE2eSupport support;
	@Autowired
	private TestPublicIds publicIds;
	@Autowired
	private ClassroomRepository classroomRepository;
	@Autowired
	private TeacherRepository teacherRepository;
	@Autowired
	private NotificationPreferenceService preferenceService;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private School school;
	private School otherSchool;
	private String year;

	private record Family(String classroom, String student, String guardian, Long parentUser, Long teacherUser,
			String teacher) {
	}

	private Family roomA;
	private Family roomB;

	@BeforeAll
	void buildSchools() {
		RestAssured.port = port;
		RestAssured.basePath = "";
		school = support.provisionSchool("Communication School");
		otherSchool = support.provisionSchool("Other Communication School");
		year = support.currentAcademicYearPublicId(school);
		roomA = family();
		roomB = family();
	}

	@BeforeEach
	void pointRestAssuredAtThisServer() {
		RestAssured.port = port;
		RestAssured.basePath = "";
	}

	// ---- announcements ----

	@Test
	void classroomAnnouncement_reachesOnlyThatClassroomsGuardians_andCountsReads() {
		String announcement = published(classroomDraft(roomA.classroom(), "IN_APP"));

		await().atMost(Duration.ofSeconds(20))
				.untilAsserted(() -> admin().get(ANNOUNCEMENTS + "/" + announcement + "/delivery")
						.then().statusCode(200)
						.body("data.dispatched", equalTo(true))
						.body("data.channels.find { it.channel == 'IN_APP' && it.status == 'SENT' }.count",
								equalTo(1)));
		admin().get(ANNOUNCEMENTS + "/" + announcement + "/delivery").then()
				.body("data.recipientCount", equalTo(1))
				.body("data.readCount", equalTo(0))
				.body("data.unreadCount", equalTo(1));
		admin().get(ANNOUNCEMENTS + "/" + announcement + "/recipients").then().statusCode(200)
				.body("data.content.userPublicId", containsInAnyOrder(publicIds.of(User.class, roomA.parentUser())));

		parent(roomA).get(ANNOUNCEMENTS + "/feed").then().statusCode(200)
				.body("data.content.publicId", hasItem(announcement));
		parent(roomB).get(ANNOUNCEMENTS + "/feed/" + announcement).then().statusCode(404);
		parent(roomB).patch(ANNOUNCEMENTS + "/feed/" + announcement + "/read").then().statusCode(404);

		long unreadBefore = parent(roomA).get(ANNOUNCEMENTS + "/feed/unread-count").then().statusCode(200)
				.extract().<Number>path("data.unreadCount").longValue();
		parent(roomA).patch(ANNOUNCEMENTS + "/feed/" + announcement + "/read").then().statusCode(204);
		parent(roomA).patch(ANNOUNCEMENTS + "/feed/" + announcement + "/read").then().statusCode(204);

		parent(roomA).get(ANNOUNCEMENTS + "/feed/unread-count").then()
				.body("data.unreadCount", equalTo((int) unreadBefore - 1));
		admin().get(ANNOUNCEMENTS + "/" + announcement + "/delivery").then()
				.body("data.readCount", equalTo(1))
				.body("data.unreadCount", equalTo(0));
	}

	@Test
	void gradeAudience_andGuardianAudience_resolveToTheRightPeople() {
		String chosen = published("""
				{"title":"For one parent","body":"Text","audienceType":"GUARDIANS","targets":["%s"],
				"channels":["IN_APP"]}
				""".formatted(roomB.guardian()));

		admin().get(ANNOUNCEMENTS + "/" + chosen + "/recipients").then()
				.body("data.content.userPublicId", containsInAnyOrder(publicIds.of(User.class, roomB.parentUser())));

		String grade = published("""
				{"title":"Grade notice","body":"Text","audienceType":"GRADE","targets":["Grade 5"],
				"channels":["IN_APP"]}
				""");

		admin().get(ANNOUNCEMENTS + "/" + grade + "/recipients?size=100").then()
				.body("data.content.userPublicId", hasItems(publicIds.of(User.class, roomA.parentUser()),
						publicIds.of(User.class, roomB.parentUser())));
	}

	@Test
	void roleAudience_reachesHoldersInThisSchoolOnly() {
		Long outsider = support.createUserWithRole(otherSchool.tenantId(), email("other-teacher"), "TEACHER");

		String announcement = published("""
				{"title":"Staff meeting","body":"Text","audienceType":"ROLE","targets":["TEACHER"],
				"channels":["IN_APP"]}
				""");

		admin().get(ANNOUNCEMENTS + "/" + announcement + "/recipients?size=100").then()
				.body("data.content.userPublicId", hasItems(publicIds.of(User.class, roomA.teacherUser()),
						publicIds.of(User.class, roomB.teacherUser())))
				.body("data.content.userPublicId", not(hasItem(publicIds.of(User.class, outsider))));
	}

	@Test
	void preferences_andAMissingWhatsAppProvider_showInTheDeliveryReport() {
		String announcement = published(classroomDraft(roomA.classroom(), "IN_APP\",\"WHATSAPP"));

		await().atMost(Duration.ofSeconds(20))
				.untilAsserted(() -> admin().get(ANNOUNCEMENTS + "/" + announcement + "/delivery")
						.then().body("data.dispatched", equalTo(true))
						.body("data.channels.find { it.channel == 'WHATSAPP' }.status", equalTo("SKIPPED"))
						.body("data.channels.find { it.channel == 'WHATSAPP' }.skipReason",
								equalTo("CHANNEL_PREFERENCE_OFF")));
	}

	@Test
	void withNoWhatsAppProvider_sendingSkipsThatChannel_andSaysSo() {
		support.inTenant(school.tenantId(), () -> preferenceService.updatePreference(roomB.parentUser(),
				NotificationTypes.ANNOUNCEMENT, Set.of(NotificationChannel.IN_APP, NotificationChannel.WHATSAPP)));

		String announcement = published(classroomDraft(roomB.classroom(), "IN_APP\",\"WHATSAPP"));

		await().atMost(Duration.ofSeconds(20))
				.untilAsserted(() -> admin().get(ANNOUNCEMENTS + "/" + announcement + "/delivery")
						.then().body("data.channels.find { it.channel == 'WHATSAPP' }.skipReason",
								equalTo("PROVIDER_NOT_CONFIGURED")));
		admin().get(ANNOUNCEMENTS + "/" + announcement + "/delivery").then()
				.body("data.channels.find { it.channel == 'IN_APP' }.status", equalTo("SENT"));
	}

	@Test
	void emergencyAlert_reachesAGuardianWhoSwitchedOptionalNotificationsOff_andIsAudited() {
		support.inTenant(school.tenantId(), () -> {
			preferenceService.updatePreference(roomA.parentUser(), SchoolNotificationTypes.EMERGENCY_ALERT, Set.of());
			preferenceService.updateSettings(roomA.parentUser(), LocalTime.MIDNIGHT, LocalTime.MAX, "UTC");
		});

		String alert = admin().body("""
				{"title":"Closure","body":"School is closed today","audienceType":"CLASSROOM","targets":["%s"]}
				""".formatted(roomA.classroom()))
				.post(ANNOUNCEMENTS + "/emergency").then().statusCode(201)
				.body("data.emergency", equalTo(true))
				.body("data.status", equalTo("PUBLISHED"))
				.extract().path("data.publicId");

		await().atMost(Duration.ofSeconds(20))
				.untilAsserted(() -> admin().get(ANNOUNCEMENTS + "/" + alert + "/delivery")
						.then().body("data.channels.find { it.channel == 'IN_APP' }.status", equalTo("SENT"))
						.body("data.channels.find { it.channel == 'EMAIL' }.status", equalTo("SENT")));
		parent(roomA).get(ANNOUNCEMENTS + "/feed").then().body("data.content[0].publicId", equalTo(alert));
		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertTrue(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM audit_logs WHERE tenant_id = ? AND resource_type = 'EmergencyAlert'",
				Long.class, school.tenantId()) > 0));
		support.inTenant(school.tenantId(), () -> preferenceService.updateSettings(roomA.parentUser(), null, null,
				"UTC"));
	}

	@Test
	void emergencyAlert_needsThePermission() {
		teacher(roomA).body("""
				{"title":"Closure","body":"Text","audienceType":"SCHOOL"}
				""").post(ANNOUNCEMENTS + "/emergency").then().statusCode(403);
	}

	@Test
	void teacher_postsOnlyToClassroomsTheyTeach() {
		String own = teacher(roomA).body(classroomDraft(roomA.classroom(), "IN_APP")).post(ANNOUNCEMENTS).then()
				.statusCode(201).extract().path("data.publicId");
		teacher(roomA).patch(ANNOUNCEMENTS + "/" + own + "/publish").then().statusCode(200);

		teacher(roomA).body(classroomDraft(roomB.classroom(), "IN_APP")).post(ANNOUNCEMENTS).then().statusCode(403);
		teacher(roomA).body("""
				{"title":"Everyone","body":"Text","audienceType":"SCHOOL","channels":["IN_APP"]}
				""").post(ANNOUNCEMENTS).then().statusCode(403);
	}

	@Test
	void lifecycle_scheduleCancelAndDelete() {
		String draft = created(classroomDraft(roomA.classroom(), "IN_APP"));
		admin().body("{\"publishAt\":\"" + Instant.now().plusSeconds(3600) + "\"}")
				.patch(ANNOUNCEMENTS + "/" + draft + "/schedule").then().statusCode(200)
				.body("data.status", equalTo("SCHEDULED"))
				.body("data.channels", hasItem("IN_APP"));
		admin().patch(ANNOUNCEMENTS + "/" + draft + "/cancel").then().statusCode(200)
				.body("data.status", equalTo("CANCELLED"));

		String other = created(classroomDraft(roomA.classroom(), "IN_APP"));
		admin().delete(ANNOUNCEMENTS + "/" + other).then().statusCode(204);
		admin().get(ANNOUNCEMENTS + "/" + other).then().statusCode(404);
	}

	@Test
	void publish_refusesAnAudienceWithNobodyInIt() {
		String empty = created(classroomDraft(classroom(), "IN_APP"));

		admin().patch(ANNOUNCEMENTS + "/" + empty + "/publish").then().statusCode(400);
		admin().get(ANNOUNCEMENTS + "/" + empty).then().body("data.status", equalTo("DRAFT"));
	}

	@Test
	void announcements_requireAuthentication() {
		RestAssured.given().header("X-Tenant-ID", school.tenantId()).get(ANNOUNCEMENTS).then().statusCode(401);
	}

	@Test
	void announcements_authoringIsForbiddenToAParent() {
		parent(roomA).get(ANNOUNCEMENTS).then().statusCode(403);
		parent(roomA).body(classroomDraft(roomA.classroom(), "IN_APP")).post(ANNOUNCEMENTS).then().statusCode(403);
	}

	@Test
	void announcements_areIsolatedByTenant() {
		String announcement = published(classroomDraft(roomA.classroom(), "IN_APP"));

		support.request(otherSchool).get(ANNOUNCEMENTS + "/" + announcement).then().statusCode(404);
		support.request(otherSchool).get(ANNOUNCEMENTS + "/" + announcement + "/delivery").then().statusCode(404);
		support.request(otherSchool).get(ANNOUNCEMENTS).then().statusCode(200).body("data.content", hasSize(0));
		support.request(otherSchool).body(classroomDraft(roomA.classroom(), "IN_APP")).post(ANNOUNCEMENTS).then()
				.statusCode(201);
		String foreignDraft = support.request(otherSchool).get(ANNOUNCEMENTS).then().extract()
				.path("data.content[0].publicId");
		support.request(otherSchool).patch(ANNOUNCEMENTS + "/" + foreignDraft + "/publish").then().statusCode(400);
	}

	// ---- messaging ----

	@Test
	void guardianAndTeacher_exchangeMessages_withUnreadCounts() {
		Family fresh = family();
		String conversation = parent(fresh).body(startBody(fresh.student(), fresh.teacher(), "Hello teacher"))
				.post(CONVERSATIONS).then().statusCode(201)
				.body("data.unreadCount", equalTo(0))
				.extract().path("data.publicId");

		teacher(fresh).get(CONVERSATIONS).then().statusCode(200)
				.body("data.content.find { it.publicId == '" + conversation + "' }.unreadCount", equalTo(1));
		long unread = teacher(fresh).get(CONVERSATIONS + "/unread-count").then().extract()
				.<Number>path("data.unreadCount").longValue();
		assertTrue(unread >= 1);

		teacher(fresh).patch(CONVERSATIONS + "/" + conversation + "/read").then().statusCode(204);
		teacher(fresh).get(CONVERSATIONS).then()
				.body("data.content.find { it.publicId == '" + conversation + "' }.unreadCount", equalTo(0));

		teacher(fresh).body("{\"body\":\"Hello parent\"}").post(CONVERSATIONS + "/" + conversation + "/messages")
				.then().statusCode(201).body("data.body", equalTo("Hello parent"));
		parent(fresh).get(CONVERSATIONS + "/" + conversation + "/messages").then().statusCode(200)
				.body("data.content[0].body", equalTo("Hello parent"))
				.body("data.content[1].body", equalTo("Hello teacher"));

		String again = parent(fresh).body(startBody(fresh.student(), fresh.teacher(), "One more"))
				.post(CONVERSATIONS).then().statusCode(201).extract().path("data.publicId");
		assertEquals(conversation, again);
	}

	@Test
	void guardian_cannotMessageATeacherWhoDoesNotTeachTheirChild() {
		parent(roomA).body(startBody(roomA.student(), roomB.teacher(), "Hello")).post(CONVERSATIONS).then()
				.statusCode(403);
	}

	@Test
	void guardian_cannotMessageAboutSomeoneElsesChild() {
		parent(roomA).body(startBody(roomB.student(), roomB.teacher(), "Hello")).post(CONVERSATIONS).then()
				.statusCode(403);
	}

	@Test
	void student_cannotTakePart() {
		support.requestWithRole(school.tenantId(), "STUDENT").get(CONVERSATIONS).then().statusCode(403);
	}

	@Test
	void outsider_cannotReadOrPostInAConversation() {
		String conversation = started(roomA, "Private");

		parent(roomB).get(CONVERSATIONS + "/" + conversation + "/messages").then().statusCode(403);
		teacher(roomB).body("{\"body\":\"Hi\"}").post(CONVERSATIONS + "/" + conversation + "/messages").then()
				.statusCode(403);
	}

	@Test
	void messaging_requiresAuthentication() {
		RestAssured.given().header("X-Tenant-ID", school.tenantId()).get(CONVERSATIONS).then().statusCode(401);
	}

	@Test
	void conversations_areIsolatedByTenant() {
		String conversation = started(roomA, "Private");

		support.request(otherSchool).get(CONVERSATIONS + "/review/" + conversation + "/messages").then()
				.statusCode(404);
		support.request(otherSchool).get(CONVERSATIONS + "/review").then().statusCode(200)
				.body("data.content", hasSize(0));
	}

	@Test
	void reviewer_readsWithAnAuditEntry_andCannotPost() {
		String conversation = started(roomA, "Please review me");
		long auditedBefore = auditedReviews(conversation);

		admin().get(CONVERSATIONS + "/review/" + conversation + "/messages").then().statusCode(200)
				.body("data.content.body", hasItem("Please review me"));

		await().atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> assertTrue(auditedReviews(conversation) > auditedBefore));
		admin().body("{\"body\":\"Reviewer speaking\"}").post(CONVERSATIONS + "/" + conversation + "/messages").then()
				.statusCode(403);
		teacher(roomA).get(CONVERSATIONS + "/review").then().statusCode(403);
	}

	@Test
	void message_canBeReportedOnce_andResolvedByAReviewer() {
		String conversation = started(roomB, "Something rude");
		String message = parent(roomB).get(CONVERSATIONS + "/" + conversation + "/messages").then().extract()
				.path("data.content[0].publicId");

		parent(roomB).body("{\"reason\":\"Mine\"}")
				.post(CONVERSATIONS + "/" + conversation + "/messages/" + message + "/report").then().statusCode(400);
		String report = teacher(roomB).body("{\"reason\":\"Rude\"}")
				.post(CONVERSATIONS + "/" + conversation + "/messages/" + message + "/report").then().statusCode(201)
				.body("data.status", equalTo("OPEN"))
				.extract().path("data.publicId");
		teacher(roomB).body("{\"reason\":\"Again\"}")
				.post(CONVERSATIONS + "/" + conversation + "/messages/" + message + "/report").then().statusCode(400);

		admin().get(CONVERSATIONS + "/review/reports?status=OPEN").then().statusCode(200)
				.body("data.content.publicId", hasItem(report));
		admin().body("{\"note\":\"Spoken to\"}").patch(CONVERSATIONS + "/review/reports/" + report + "/resolve").then()
				.statusCode(200).body("data.status", equalTo("RESOLVED"));
	}

	@Test
	void settings_switchMessagingOff_andLimitItsHours() {
		String conversation = started(roomA, "Before the switch");
		LocalTime now = LocalTime.now(java.time.ZoneOffset.UTC);

		admin().body("{\"enabled\":false}").put("/api/v1/messaging-settings").then().statusCode(200)
				.body("data.enabled", equalTo(false))
				.body("data.hoursStart", nullValue());
		parent(roomA).body("{\"body\":\"Hi\"}").post(CONVERSATIONS + "/" + conversation + "/messages").then()
				.statusCode(400);

		String closed = """
				{"enabled":true,"hoursStart":"%s","hoursEnd":"%s"}
				""".formatted(now.plusHours(2).withNano(0), now.plusHours(3).withNano(0));
		admin().body(closed).put("/api/v1/messaging-settings").then().statusCode(200);
		parent(roomA).body("{\"body\":\"Hi\"}").post(CONVERSATIONS + "/" + conversation + "/messages").then()
				.statusCode(400);

		admin().body("{\"enabled\":true}").put("/api/v1/messaging-settings").then().statusCode(200);
		parent(roomA).body("{\"body\":\"Hi\"}").post(CONVERSATIONS + "/" + conversation + "/messages").then()
				.statusCode(201);
	}

	// ---- fixtures ----

	private Family family() {
		String classroom = classroom();
		String student = support.createStudent(school);
		admin().body("{\"studentPublicId\":\"" + student + "\",\"academicYearPublicId\":\"" + year + "\"}")
				.post("/api/v1/classrooms/" + classroom + "/students").then().statusCode(HttpStatus.CREATED.value());
		Long parentUser = support.createUserWithRole(school.tenantId(), email("parent"), "PARENT");
		String guardian = admin()
				.body("{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"email\":\"" + email("guardian")
						+ "\",\"phone\":\"+14155552671\",\"userPublicId\":\"" + publicIds.of(User.class, parentUser)
						+ "\"}")
				.post("/api/v1/guardians").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
		admin().body(
				"{\"studentPublicId\":\"" + student + "\",\"relationshipType\":\"MOTHER\",\"primaryContact\":true}")
				.post("/api/v1/guardians/" + guardian + "/students").then().statusCode(HttpStatus.CREATED.value());
		Long teacherUser = support.createUserWithRole(school.tenantId(), email("teacher"), "TEACHER");
		Long teacherId = leadClassroom(classroom, teacherUser);
		return new Family(classroom, student, guardian, parentUser, teacherUser,
				publicIds.of(Employee.class, teacherId));
	}

	private Long leadClassroom(String classroomPublicId, Long teacherUser) {
		return support.inTenant(school.tenantId(), () -> {
			Teacher teacher = Teacher.create("T-" + suffix(), "Class", "Teacher", email("staff"),
					java.time.LocalDate.of(2024, 1, 1));
			teacher.setUserId(teacherUser);
			Teacher saved = teacherRepository.save(teacher);
			var room = classroomRepository
					.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), school.tenantId()).orElseThrow();
			room.reassignTeacher(saved.getId());
			classroomRepository.save(room);
			return saved.getId();
		});
	}

	private String classroom() {
		return admin().body("{\"classCode\":\"C-" + suffix() + "\",\"grade\":\"Grade 5\",\"section\":\"A\","
				+ "\"academicYearPublicId\":\"" + year + "\"}")
				.post("/api/v1/classrooms").then().statusCode(HttpStatus.CREATED.value()).extract()
				.path("data.publicId");
	}

	private static String classroomDraft(String classroom, String channels) {
		return """
				{"title":"Class notice","body":"Bring your books","audienceType":"CLASSROOM","targets":["%s"],
				"channels":["%s"]}
				""".formatted(classroom, channels);
	}

	private String created(String body) {
		return admin().body(body).post(ANNOUNCEMENTS).then().statusCode(201).extract().path("data.publicId");
	}

	private String published(String body) {
		String announcement = created(body);
		admin().patch(ANNOUNCEMENTS + "/" + announcement + "/publish").then().statusCode(200)
				.body("data.status", equalTo("PUBLISHED"));
		return announcement;
	}

	private String started(Family family, String body) {
		return parent(family).body(startBody(family.student(), family.teacher(), body)).post(CONVERSATIONS).then()
				.statusCode(201).extract().path("data.publicId");
	}

	private static String startBody(String student, String teacher, String body) {
		return """
				{"studentPublicId":"%s","teacherPublicId":"%s","body":"%s"}
				""".formatted(student, teacher, body);
	}

	private long auditedReviews(String conversation) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM audit_logs WHERE tenant_id = ? AND resource_id = ? AND action = 'SENSITIVE_ACCESS'",
				Long.class, school.tenantId(), conversation);
	}

	private RequestSpecification admin() {
		return support.request(school);
	}

	private RequestSpecification parent(Family family) {
		return support.requestAsUser(school.tenantId(), family.parentUser(), "PARENT");
	}

	private RequestSpecification teacher(Family family) {
		return support.requestAsUser(school.tenantId(), family.teacherUser(), "TEACHER");
	}

	private static String suffix() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	private static String email(String prefix) {
		return prefix + "-" + suffix() + "@school.test";
	}
}
