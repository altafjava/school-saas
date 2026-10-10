package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.security.PasswordEncoder;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserStatus;
import com.altafjava.platform.domain.user.repository.RoleRepository;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.AssignmentService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.LessonService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.application.service.SubmissionService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.lms.model.Assignment;
import com.altafjava.school.domain.lms.model.Lesson;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/**
 * Verifies that lessons/assignments created under tenant A are not visible to tenant B, and that
 * a TEACHER caller must actually be the assigned class teacher of the target classroom.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class LmsTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private LessonService lessonService;

	@Autowired
	private AssignmentService assignmentService;

	@Autowired
	private ClassroomService classroomService;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private SubjectService subjectService;

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private StudentService studentService;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private SubmissionService submissionService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "lms-a-" + suffix, 1L, "admin@lms-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "lms-b-" + suffix, 1L, "admin@lms-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private void authenticateAsTenantAdmin() {
		AuthenticatedUser principal = fixedIdPrincipal(-1L);
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}

	private void authenticateAsTeacher(Long userId) {
		AuthenticatedUser principal = fixedIdPrincipal(userId);
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_TEACHER"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}

	private void authenticateAsStudent(Long userId) {
		AuthenticatedUser principal = fixedIdPrincipal(userId);
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}

	private AuthenticatedUser fixedIdPrincipal(Long userId) {
		return new AuthenticatedUser() {
			@Override
			public Long getId() {
				return userId;
			}

			@Override
			public String getUsername() {
				return "user-" + userId;
			}

			@Override
			public Long getTenantId() {
				return null;
			}
		};
	}

	private record ClassroomFixture(Classroom classroom, Subject subject, Long teacherUserId) {
	}

	// TEACHER/STUDENT are global roles (tenant_id = NULL) — see Role.java. teachers.user_id and
	// students.user_id carry a real FK to users.id, so the linked account must actually exist,
	// matching AcademicScopeE2ETest's teacherUser() idiom.
	private Long createUserWithRole(String email, String roleName) {
		var role = roleRepository.findAll().stream()
				.filter(r -> r.getTenantId() == null && roleName.equals(r.getName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));
		User user = User.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode("Password123!"))
				.status(UserStatus.ACTIVE)
				.emailVerified(true)
				.build();
		user.addRole(role);
		return userRepository.save(user).getId();
	}

	// Caller must have already activated the target tenant via activateTenant(...) before calling
	// this — it does not switch tenants itself, only creates fixture data within the active one.
	private ClassroomFixture createClassroomWithTeacher(String suffix) {
		authenticateAsTenantAdmin();
		var academicYear = academicYearService.create("2024-25-" + suffix, LocalDate.of(2024, 6, 1),
				LocalDate.of(2025, 5, 31), true);
		String email = "jane-" + suffix + "@school.test";
		Teacher teacher = teacherService.hire("EMP-" + suffix, "Jane", "Doe", email, LocalDate.of(2020, 8, 1));
		Long teacherUserId = createUserWithRole(email, "TEACHER");
		teacher.setUserId(teacherUserId);
		teacherRepository.save(teacher);
		Classroom classroom = classroomService.create("CLS-" + suffix, "Grade 5", "A",
				academicYear.getPublicId().toString(), teacher.getPublicId().toString());
		Subject subject = subjectService.create("SUB-" + suffix, "Science", null);
		return new ClassroomFixture(classroom, subject, teacherUserId);
	}

	@Test
	void lessonPostedUnderTenantA_isNotVisibleWhenListingTenantBClassroom() {
		activateTenant(tenantA);
		ClassroomFixture fixtureA = createClassroomWithTeacher("lsa-" + UUID.randomUUID().toString().substring(0, 6));
		authenticateAsTeacher(fixtureA.teacherUserId());
		lessonService.post(fixtureA.classroom().getPublicId().toString(), fixtureA.subject().getPublicId().toString(),
				"Intro to Cells", "Chapter 1", null);

		activateTenant(tenantB);
		ClassroomFixture fixtureB = createClassroomWithTeacher("lsb-" + UUID.randomUUID().toString().substring(0, 6));
		authenticateAsTenantAdmin();
		Page<Lesson> lessonsB = lessonService.listByClassroom(fixtureB.classroom().getPublicId().toString(),
				PageRequest.of(0, 100));

		assertTrue(lessonsB.getContent().isEmpty(), "Tenant B's classroom must not see tenant A's lessons");
	}

	@Test
	void assignmentCreatedUnderTenantA_notAccessibleForRescheduleFromTenantB() {
		activateTenant(tenantA);
		ClassroomFixture fixtureA = createClassroomWithTeacher("asa-" + UUID.randomUUID().toString().substring(0, 6));
		authenticateAsTeacher(fixtureA.teacherUserId());
		Assignment assignment = assignmentService.create(fixtureA.classroom().getPublicId().toString(),
				fixtureA.subject().getPublicId().toString(), "Essay", "Write about rivers", null,
				LocalDate.now().plusDays(7), BigDecimal.valueOf(100));
		String assignmentPublicId = assignment.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> assignmentService.reschedule(assignmentPublicId, LocalDate.now().plusDays(10)),
				"Tenant B must receive ResourceNotFoundException for tenant A's assignment");
	}

	@Test
	void teacherNotAssignedToClassroom_cannotPostLesson() {
		activateTenant(tenantA);
		ClassroomFixture fixtureA = createClassroomWithTeacher("nta-" + UUID.randomUUID().toString().substring(0, 6));
		// A second, unrelated teacher who is not the class teacher of fixtureA's classroom.
		authenticateAsTenantAdmin();
		String outsiderEmail = "sam-" + UUID.randomUUID().toString().substring(0, 6) + "@school.test";
		Teacher outsider = teacherService.hire("EMP-OUT-" + UUID.randomUUID().toString().substring(0, 6), "Sam",
				"Lee", outsiderEmail, LocalDate.of(2021, 1, 1));
		Long outsiderUserId = createUserWithRole(outsiderEmail, "TEACHER");
		outsider.setUserId(outsiderUserId);
		teacherRepository.save(outsider);

		authenticateAsTeacher(outsiderUserId);
		String classroomPublicId = fixtureA.classroom().getPublicId().toString();
		String subjectPublicId = fixtureA.subject().getPublicId().toString();

		assertThrows(AccessDeniedException.class,
				() -> lessonService.post(classroomPublicId, subjectPublicId, "Not allowed", null, null));
	}

	@Test
	void assignmentListForTenantAClassroom_isolatedFromTenantB() {
		activateTenant(tenantA);
		ClassroomFixture fixtureA = createClassroomWithTeacher("lia-" + UUID.randomUUID().toString().substring(0, 6));
		authenticateAsTeacher(fixtureA.teacherUserId());
		assignmentService.create(fixtureA.classroom().getPublicId().toString(),
				fixtureA.subject().getPublicId().toString(), "Homework 1", null, null, LocalDate.now().plusDays(3),
				null);

		activateTenant(tenantB);
		ClassroomFixture fixtureB = createClassroomWithTeacher("lib-" + UUID.randomUUID().toString().substring(0, 6));
		authenticateAsTenantAdmin();
		Page<Assignment> assignmentsB = assignmentService.listByClassroom(
				fixtureB.classroom().getPublicId().toString(), PageRequest.of(0, 100));

		assertFalse(assignmentsB.getContent().stream()
				.anyMatch(a -> tenantA.getId().equals(a.getTenantId())),
				"Tenant B must not see tenant A's assignments");
		assertEquals(0, assignmentsB.getContent().size());
	}

	@Test
	void assignmentCreatedUnderTenantA_notGradableFromTenantB() {
		activateTenant(tenantA);
		ClassroomFixture fixtureA = createClassroomWithTeacher("gra-" + UUID.randomUUID().toString().substring(0, 6));
		authenticateAsTeacher(fixtureA.teacherUserId());
		Assignment assignment = assignmentService.create(fixtureA.classroom().getPublicId().toString(),
				fixtureA.subject().getPublicId().toString(), "Essay", null, null, LocalDate.now().plusDays(7), null);
		String assignmentPublicId = assignment.getPublicId().toString();

		activateTenant(tenantB);
		assertThrows(ResourceNotFoundException.class,
				() -> submissionService.grade(assignmentPublicId, UUID.randomUUID().toString(),
						BigDecimal.valueOf(50), null),
				"Tenant B must receive ResourceNotFoundException for tenant A's assignment");
	}

	@Test
	void studentNotOnClassroomRoster_cannotSubmitAssignment() {
		activateTenant(tenantA);
		ClassroomFixture fixtureA = createClassroomWithTeacher("rst-" + UUID.randomUUID().toString().substring(0, 6));
		authenticateAsTeacher(fixtureA.teacherUserId());
		Assignment assignment = assignmentService.create(fixtureA.classroom().getPublicId().toString(),
				fixtureA.subject().getPublicId().toString(), "Essay", null, null, LocalDate.now().plusDays(7), null);
		String studentEmail = "nora-" + UUID.randomUUID().toString().substring(0, 6) + "@school.test";
		Student student = studentService.enroll("STU-" + UUID.randomUUID().toString().substring(0, 6), "Nora",
				"Kim", studentEmail, LocalDate.of(2011, 4, 4));
		// Deliberately NOT enrolled in fixtureA's classroom roster.
		Long studentUserId = createUserWithRole(studentEmail, "STUDENT");
		student.setUserId(studentUserId);
		studentRepository.save(student);
		String assignmentPublicId = assignment.getPublicId().toString();

		authenticateAsStudent(studentUserId);
		assertThrows(ResourceNotFoundException.class,
				() -> submissionService.submit(assignmentPublicId, null, "My answer"),
				"A student not on the classroom roster must not be able to submit work for it");
	}
}
