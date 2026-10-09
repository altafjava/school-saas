package com.altafjava.school.demo;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.security.PasswordEncoder;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantContextSnapshot;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserStatus;
import com.altafjava.platform.domain.user.repository.RoleRepository;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.DepartmentService;
import com.altafjava.school.application.service.EmployeeService;
import com.altafjava.school.application.service.ExamService;
import com.altafjava.school.application.service.ExamTypeDefinitionService;
import com.altafjava.school.application.service.FeeAssignmentService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.application.service.GradeService;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.term.model.Term;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds one realistic school, "Demo School", for frontend development and end-to-end tests: an
 * academic year with two terms, departments, subjects, classrooms with class teachers, teachers and
 * other staff, students with guardians (some siblings), fees, graded exams and recent attendance —
 * plus a login for each role. Idempotent: does nothing when the tenant already exists. Never active
 * in production: it only exists under the {@code dev} and {@code test} profiles.
 */
@Slf4j
@Service
@Profile({ "dev", "test" })
@RequiredArgsConstructor
public class DemoDataSeeder {

	public static final String SUBDOMAIN = "demo";
	public static final String ADMIN_EMAIL = "admin@demo.school";
	public static final String PASSWORD = "Demo@12345";

	private static final long PLAN_BASIC = 2L;
	private static final String[] GIVEN = { "Aarav", "Diya", "Vivaan", "Anaya", "Aditya", "Isha", "Arjun", "Meera",
			"Kabir", "Saanvi", "Reyansh", "Myra", "Ishaan", "Kiara", "Ayaan", "Navya", "Krish", "Aadhya" };
	private static final String[] FAMILY = { "Sharma", "Patel", "Singh", "Gupta", "Reddy", "Nair", "Khan", "Joshi",
			"Mehta", "Iyer", "Das", "Bose" };
	private static final String[][] SUBJECTS = { { "ENG", "English" }, { "MATH", "Mathematics" },
			{ "SCI", "Science" }, { "SST", "Social Studies" }, { "HIN", "Hindi" }, { "CS", "Computer Science" } };
	private static final int STUDENTS_PER_CLASS = 6;
	private static final int ATTENDANCE_DAYS = 5;

	private final TenantOnboardingService onboardingService;
	private final TenantRepository tenantRepository;
	private final AcademicYearService academicYearService;
	private final TermService termService;
	private final DepartmentService departmentService;
	private final SubjectService subjectService;
	private final TeacherService teacherService;
	private final EmployeeService employeeService;
	private final ClassroomService classroomService;
	private final StudentService studentService;
	private final GuardianService guardianService;
	private final FeeStructureService feeStructureService;
	private final FeeAssignmentService feeAssignmentService;
	private final ExamTypeDefinitionService examTypeDefinitionService;
	private final ExamService examService;
	private final GradeService gradeService;
	private final AttendanceService attendanceService;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final StudentRepository studentRepository;
	private final TeacherRepository teacherRepository;
	private final GuardianRepository guardianRepository;
	private final PasswordEncoder passwordEncoder;
	private final PlatformTransactionManager transactionManager;

	/** @return the demo tenant, newly created or already present */
	public Tenant seed() {
		var existing = tenantRepository.findBySubdomain(SUBDOMAIN);
		if (existing.isPresent()) {
			log.info("action=demo_seed_skipped reason=tenant-exists subdomain={}", SUBDOMAIN);
			return existing.get();
		}
		Tenant tenant = onboardingService.registerTenant(
				new RegisterTenantCommand("Demo School", SUBDOMAIN, PLAN_BASIC, ADMIN_EMAIL, PASSWORD, "INR"));
		TenantContext.runAsTenant(new TenantContextSnapshot(tenant.getId(), tenant.getPublicId(),
				tenant.getSubdomain(), tenant.getType(), null), this::seedTenant);
		log.info("action=demo_seed_completed subdomain={} adminEmail={}", SUBDOMAIN, ADMIN_EMAIL);
		return tenant;
	}

	private void seedTenant() {
		Random random = new Random(42);
		LocalDate today = LocalDate.now();
		int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
		AcademicYear year = academicYearService.create(startYear + "-" + (startYear + 1 - 2000),
				LocalDate.of(startYear, 4, 1), LocalDate.of(startYear + 1, 3, 31), true);
		Term term1 = termService.create("Term 1", LocalDate.of(startYear, 4, 1), LocalDate.of(startYear, 9, 30),
				year.getId());
		termService.create("Term 2", LocalDate.of(startYear, 10, 1), LocalDate.of(startYear + 1, 3, 31),
				year.getId());

		for (String[] dept : new String[][] { { "Academics", "ACAD" }, { "Administration", "ADMIN" },
				{ "Operations", "OPS" } }) {
			departmentService.create(dept[0], dept[1], null);
		}
		List<Subject> subjects = new ArrayList<>();
		for (String[] s : SUBJECTS) {
			subjects.add(subjectService.create(s[0], s[1], null));
		}

		List<Teacher> teachers = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			teachers.add(teacherService.hire(null, GIVEN[i], FAMILY[i], "teacher" + (i + 1) + "@demo.school",
					today.minusYears(2 + i)));
		}
		employeeService.hire(StaffCategory.ADMINISTRATIVE, null, "Ravi", "Kumar", "clerk@demo.school",
				today.minusYears(3));
		employeeService.hire(StaffCategory.SUPPORT, null, "Suresh", "Yadav", "driver@demo.school",
				today.minusYears(1));

		List<Classroom> classrooms = new ArrayList<>();
		int teacherIndex = 0;
		for (int grade = 1; grade <= 3; grade++) {
			for (String section : new String[] { "A", "B" }) {
				classrooms.add(classroomService.create("G" + grade + section, "Grade " + grade, section,
						year.getPublicId().toString(), teachers.get(teacherIndex++).getId()));
			}
		}

		List<Student> students = new ArrayList<>();
		for (int c = 0; c < classrooms.size(); c++) {
			for (int n = 0; n < STUDENTS_PER_CLASS; n++) {
				int i = students.size();
				Student student = studentService.enroll(null, GIVEN[i % GIVEN.length], FAMILY[(i / 2) % FAMILY.length],
						"student" + (i + 1) + "@demo.school", LocalDate.of(2020 - c / 2, 1 + i % 12, 1 + i % 27));
				classroomService.enrollStudent(classrooms.get(c).getPublicId().toString(),
						student.getPublicId().toString(), year.getPublicId().toString());
				students.add(student);
			}
		}
		List<Guardian> guardians = seedGuardians(students);

		seedFees(classrooms);
		seedExamsAndGrades(random, term1, subjects, classrooms, students);
		seedAttendance(random, classrooms, students, today);
		seedLogins(teachers.get(0), students.get(0), guardians.get(0));
	}

	/** Each of the first twelve guardians has two children (siblings); the rest have one. */
	private List<Guardian> seedGuardians(List<Student> students) {
		List<Guardian> guardians = new ArrayList<>();
		int guardianCount = students.size() * 2 / 3;
		for (int g = 0; g < guardianCount; g++) {
			Student first = students.get(g);
			Guardian guardian = guardianService.create(GIVEN[(g + 7) % GIVEN.length], first.getLastName(),
					"parent" + (g + 1) + "@demo.school", String.format("+9198765%05d", g), null);
			guardianService.linkToStudent(guardian.getPublicId().toString(), first.getPublicId().toString(),
					g % 2 == 0 ? RelationshipType.MOTHER : RelationshipType.FATHER, true);
			if (g < students.size() - guardianCount) {
				guardianService.linkToStudent(guardian.getPublicId().toString(),
						students.get(guardianCount + g).getPublicId().toString(), RelationshipType.MOTHER, true);
			}
			guardians.add(guardian);
		}
		return guardians;
	}

	private void seedFees(List<Classroom> classrooms) {
		FeeStructure tuition = feeStructureService.create("Tuition", new BigDecimal("5000"), FeeFrequency.MONTHLY,
				"STANDARD");
		FeeStructure annual = feeStructureService.create("Annual charges", new BigDecimal("12000"),
				FeeFrequency.ANNUAL, "STANDARD");
		feeStructureService.create("Transport", new BigDecimal("1500"), FeeFrequency.MONTHLY, "OPTIONAL");
		for (Classroom classroom : classrooms) {
			feeAssignmentService.assign(tuition.getPublicId().toString(), null, classroom.getPublicId().toString());
			feeAssignmentService.assign(annual.getPublicId().toString(), null, classroom.getPublicId().toString());
		}
	}

	private void seedExamsAndGrades(Random random, Term term, List<Subject> subjects, List<Classroom> classrooms,
			List<Student> students) {
		List<ExamTypeDefinition> types = examTypeDefinitionService.listActive();
		ExamTypeDefinition type = types.isEmpty() ? examTypeDefinitionService.create("UNIT_TEST", "Unit test", 1)
				: types.get(0);
		LocalDateTime when = LocalDateTime.now().minusDays(14);
		for (int c = 0; c < classrooms.size(); c++) {
			Classroom classroom = classrooms.get(c);
			for (int s = 0; s < 2; s++) {
				Subject subject = subjects.get((c + s) % subjects.size());
				Exam exam = examService.schedule(subject.getName() + " unit test", subject.getId(), classroom.getId(),
						when, new BigDecimal("50"), term.getId(), type.getId(), new BigDecimal("20"));
				for (int n = 0; n < STUDENTS_PER_CLASS; n++) {
					Student student = students.get(c * STUDENTS_PER_CLASS + n);
					gradeService.record(student.getId(), exam.getId(), BigDecimal.valueOf(20 + random.nextInt(31)),
							"demo-seed");
				}
				examService.complete(exam.getPublicId().toString());
				examService.publishResults(exam.getPublicId().toString(), "demo-seed");
			}
		}
	}

	private void seedAttendance(Random random, List<Classroom> classrooms, List<Student> students, LocalDate today) {
		List<LocalDate> days = new ArrayList<>();
		for (LocalDate day = today.minusDays(1); days.size() < ATTENDANCE_DAYS; day = day.minusDays(1)) {
			if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
				days.add(day);
			}
		}
		for (int c = 0; c < classrooms.size(); c++) {
			for (int n = 0; n < STUDENTS_PER_CLASS; n++) {
				Student student = students.get(c * STUDENTS_PER_CLASS + n);
				for (LocalDate day : days) {
					int roll = random.nextInt(20);
					AttendanceStatus status = roll == 0 ? AttendanceStatus.ABSENT
							: roll == 1 ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
					attendanceService.mark(student.getId(), classrooms.get(c).getId(), day, status, "demo-seed");
				}
			}
		}
	}

	/** A login per role; the teacher, student and parent are tied to real records so their self-service works. */
	private void seedLogins(Teacher teacher, Student student, Guardian guardian) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			createUser("principal@demo.school", "Priya", "Menon", SchoolRoles.PRINCIPAL);
			createUser("finance@demo.school", "Farhan", "Ali", SchoolRoles.FINANCE);
			createUser("hr@demo.school", "Hema", "Rao", SchoolRoles.HR);
			createUser("academic@demo.school", "Anil", "Verma", SchoolRoles.ACADEMIC);
			teacher.setUserId(createUser("teacher@demo.school", teacher.getFirstName(), teacher.getLastName(),
					SchoolRoles.TEACHER).getId());
			teacherRepository.save(teacher);
			student.setUserId(createUser("student@demo.school", student.getFirstName(), student.getLastName(),
					SchoolRoles.STUDENT).getId());
			studentRepository.save(student);
			guardian.linkUserAccount(createUser("parent@demo.school", guardian.getFirstName(),
					guardian.getLastName(), SchoolRoles.PARENT).getId());
			guardianRepository.save(guardian);
		});
		log.info(
				"action=demo_logins_created password={} roles=admin,principal,finance,hr,academic,teacher,student,parent",
				PASSWORD);
	}

	private User createUser(String email, String firstName, String lastName, String roleName) {
		User user = User.builder()
				.email(email)
				.firstName(firstName)
				.lastName(lastName)
				.passwordHash(passwordEncoder.encode(PASSWORD))
				.status(UserStatus.ACTIVE)
				.emailVerified(true)
				.build();
		roleRepository.findByName(roleName).ifPresent(user::addRole);
		return userRepository.save(user);
	}
}
