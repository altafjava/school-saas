package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.security.PasswordEncoder;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserStatus;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.application.policy.SchoolResourceAccessPolicy;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/**
 * Verifies that SchoolResourceAccessPolicy is registered with the platform enforcer
 * and that teacher-classroom access restriction rules are applied.
 *
 * Phase 5 validation: ResourceAccessPolicy SPI is discovered and invoked by the platform.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class SchoolResourceAccessPolicyIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private SchoolResourceAccessPolicy schoolResourceAccessPolicy;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private ClassroomRepository classroomRepository;

	@Autowired
	private AcademicYearRepository academicYearRepository;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private GuardianRepository guardianRepository;

	@Autowired
	private StudentGuardianLinkRepository studentGuardianLinkRepository;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private Long testTenantId;
	private Long academicYearId;

	@BeforeEach
	void createTenantContext() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Policy Test School", "policy-" + suffix, 1L,
				"admin@policy.test", "Password123!", "USD"));
		testTenantId = tenant.getId();
		TenantContext.ForTesting.setCurrentTenant(testTenantId, tenant.getPublicId(), "policy-" + suffix,
				TenantType.SHARED);
		academicYearId = academicYearRepository.save(AcademicYear.create("2024-25",
				LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), true)).getId();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void schoolResourceAccessPolicy_isRegisteredAsSpringBean() {
		assertNotNull(schoolResourceAccessPolicy,
				"SchoolResourceAccessPolicy must be discovered as a Spring bean via @Component");
	}

	@Test
	void teacher_canAccessOwnClassroom() {
		// classroom.classTeacherId is a Teacher.id, not a platform users.id — the policy must
		// bridge the acting user to their own Teacher record via Teacher.userId (added Phase 2)
		// before comparing, so this needs a real linked User, not the Teacher's own raw id.
		Long teacherUserId = createUser("johnson-" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu");
		Teacher teacher = teacherRepository.save(Teacher.create(
				"EMP-" + UUID.randomUUID().toString().substring(0, 6),
				"Ms", "Johnson", "johnson@test.edu", null));
		teacher.setUserId(teacherUserId);
		teacherRepository.save(teacher);
		Classroom classroom = classroomRepository.save(Classroom.create(
				"CLS-" + UUID.randomUUID().toString().substring(0, 6),
				"Grade 5", "A", academicYearId, "2024-25", teacher.getId()));

		boolean allowed = schoolResourceAccessPolicy.isAllowed(
				String.valueOf(teacherUserId), testTenantId, "CLASSROOM", classroom.getPublicId().toString(), "READ");
		assertTrue(allowed, "Teacher must be allowed to READ their own classroom");
	}

	@Test
	void teacher_cannotAccessAnotherTeachersClassroom() {
		Long teacher1UserId = createUser("smith-" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu");
		Long teacher2UserId = createUser("lee-" + UUID.randomUUID().toString().substring(0, 6) + "@test.edu");
		Teacher teacher1 = teacherRepository.save(Teacher.create(
				"EMP-" + UUID.randomUUID().toString().substring(0, 6),
				"Mr", "Smith", "smith@test.edu", null));
		teacher1.setUserId(teacher1UserId);
		teacherRepository.save(teacher1);
		Teacher teacher2 = teacherRepository.save(Teacher.create(
				"EMP-" + UUID.randomUUID().toString().substring(0, 6),
				"Mrs", "Lee", "lee@test.edu", null));
		teacher2.setUserId(teacher2UserId);
		teacherRepository.save(teacher2);
		Classroom classroom = classroomRepository.save(Classroom.create(
				"CLS-" + UUID.randomUUID().toString().substring(0, 6),
				"Grade 6", "B", academicYearId, "2024-25", teacher1.getId()));

		boolean allowed = schoolResourceAccessPolicy.isAllowed(
				String.valueOf(teacher2UserId), testTenantId, "CLASSROOM", classroom.getPublicId().toString(),
				"READ");
		assertFalse(allowed, "Teacher2 must be denied READ access to teacher1's classroom");
	}

	@Test
	void teacher_withNoLinkedUserAccount_cannotAccessTheirOwnClassroom() {
		// A teacher record not yet linked to a login fails closed; unit coverage is TeachingAssignmentResolverTest.
		Teacher teacher = teacherRepository.save(Teacher.create(
				"EMP-" + UUID.randomUUID().toString().substring(0, 6),
				"Mr", "Unlinked", "unlinked@test.edu", null));
		Classroom classroom = classroomRepository.save(Classroom.create(
				"CLS-" + UUID.randomUUID().toString().substring(0, 6),
				"Grade 7", "C", academicYearId, "2024-25", teacher.getId()));

		boolean allowed = schoolResourceAccessPolicy.isAllowed(
				String.valueOf(teacher.getId()), testTenantId, "CLASSROOM", classroom.getPublicId().toString(),
				"READ");
		assertFalse(allowed,
				"A caller with no Teacher.userId link must be denied, even if their raw id happens to equal "
						+ "the classroom's classTeacherId");
	}

	@Test
	void resourceTypesNotManagedByThePolicy_areAllowedByDefault() {
		boolean allowed = schoolResourceAccessPolicy.isAllowed(
				"any-user", testTenantId, "EXAM", "any-public-id", "READ");
		assertTrue(allowed, "Resource types not managed by the school policy must default to allowed");
	}

	@Test
	void student_canAccessOwnData() {
		Long selfUserId = createUser("student-self@test.edu");
		Student student = studentRepository.save(Student.create(
				"STU-" + UUID.randomUUID().toString().substring(0, 6), "Alice", "Smith", "alice@test.edu", null));
		student.setUserId(selfUserId);
		studentRepository.save(student);

		boolean allowed = schoolResourceAccessPolicy.isAllowed(
				String.valueOf(selfUserId), testTenantId, "STUDENT", student.getPublicId().toString(), "READ");
		assertTrue(allowed, "Student must be allowed to READ their own data");
	}

	@Test
	void student_cannotAccessAnotherStudentsData() {
		Long ownerUserId = createUser("student-owner@test.edu");
		Long otherUserId = createUser("student-other@test.edu");
		Student student = studentRepository.save(Student.create(
				"STU-" + UUID.randomUUID().toString().substring(0, 6), "Bob", "Jones", "bob@test.edu", null));
		student.setUserId(ownerUserId);
		studentRepository.save(student);

		boolean allowed = schoolResourceAccessPolicy.isAllowed(
				String.valueOf(otherUserId), testTenantId, "STUDENT", student.getPublicId().toString(), "READ");
		assertFalse(allowed, "A different student's user ID must be denied READ access");
	}

	@Test
	void guardian_canAccessLinkedChildsData() {
		Long guardianUserId = createUser("guardian-linked@test.edu");
		Student student = studentRepository.save(Student.create(
				"STU-" + UUID.randomUUID().toString().substring(0, 6), "Carol", "White", "carol@test.edu", null));
		Guardian guardian = guardianRepository.save(
				Guardian.create("Jane", "Doe", "jane@test.edu", "555-0100", guardianUserId));
		studentGuardianLinkRepository.save(
				StudentGuardianLink.create(student.getId(), guardian.getId(), RelationshipType.MOTHER, true));

		boolean allowed = schoolResourceAccessPolicy.isAllowed(
				String.valueOf(guardianUserId), testTenantId, "STUDENT", student.getPublicId().toString(), "READ");
		assertTrue(allowed, "A linked guardian must be allowed to READ their child's data");
	}

	@Test
	void guardian_cannotAccessUnlinkedStudentsData() {
		Long guardianUserId = createUser("guardian-unlinked@test.edu");
		Student student = studentRepository.save(Student.create(
				"STU-" + UUID.randomUUID().toString().substring(0, 6), "Dan", "Brown", "dan@test.edu", null));
		guardianRepository.save(Guardian.create("Jane", "Doe", "jane2@test.edu", "555-0100", guardianUserId));

		boolean allowed = schoolResourceAccessPolicy.isAllowed(
				String.valueOf(guardianUserId), testTenantId, "STUDENT", student.getPublicId().toString(), "READ");
		assertFalse(allowed, "A guardian with no link to this student must be denied READ access");
	}

	private Long createUser(String email) {
		User user = User.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode("Password123!"))
				.status(UserStatus.ACTIVE)
				.emailVerified(true)
				.build();
		return userRepository.save(user).getId();
	}
}
