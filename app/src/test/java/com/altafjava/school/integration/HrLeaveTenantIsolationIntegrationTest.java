package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.altafjava.school.application.service.DepartmentService;
import com.altafjava.school.application.service.LeaveBalanceService;
import com.altafjava.school.application.service.LeaveRequestService;
import com.altafjava.school.application.service.LeaveTypeService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.leave.model.LeaveBalance;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/**
 * Verifies that departments/leave types/leave requests created under tenant A are not visible or
 * actionable from tenant B, and that a TEACHER caller can only cancel their own leave requests.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class HrLeaveTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private DepartmentService departmentService;

	@Autowired
	private LeaveTypeService leaveTypeService;

	@Autowired
	private LeaveRequestService leaveRequestService;

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private AcademicYearService academicYearService;

	@Autowired
	private LeaveBalanceService leaveBalanceService;

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
				"School A", "hrleave-a-" + suffix, 1L, "admin@hrleave-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "hrleave-b-" + suffix, 1L, "admin@hrleave-b.test", "Password123!", "USD"));
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

	private record TeacherFixture(Teacher teacher, Long teacherUserId, LeaveType leaveType,
			AcademicYear academicYear) {
	}

	// Caller must have already activated the target tenant via activateTenant(...) before calling
	// this — it does not switch tenants itself, only creates fixture data within the active one.
	private TeacherFixture createTeacherWithLeaveType(String suffix) {
		authenticateAsTenantAdmin();
		var academicYear = academicYearService.create("2026-27-" + suffix, LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31), true);
		String email = "jane-" + suffix + "@school.test";
		Teacher teacher = teacherService.hire("EMP-" + suffix, "Jane", "Doe", email, LocalDate.of(2020, 8, 1));
		Long teacherUserId = createUserWithRole(email, "TEACHER");
		teacher.setUserId(teacherUserId);
		teacherRepository.save(teacher);
		LeaveType leaveType = leaveTypeService.create("Sick Leave-" + suffix, BigDecimal.valueOf(12));
		return new TeacherFixture(teacher, teacherUserId, leaveType, academicYear);
	}

	@Test
	void departmentCreatedUnderTenantA_isNotVisibleWhenListingTenantB() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		departmentService.create("Science-" + UUID.randomUUID(), "SCI-" + UUID.randomUUID().toString().substring(0, 6),
				null);

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		Page<Department> departmentsB = departmentService.list(PageRequest.of(0, 100));

		assertTrue(departmentsB.getContent().isEmpty(), "Tenant B must not see tenant A's departments");
	}

	@Test
	void leaveRequestSubmittedUnderTenantA_notApprovableFromTenantB() {
		activateTenant(tenantA);
		TeacherFixture fixtureA = createTeacherWithLeaveType("lra-" + UUID.randomUUID().toString().substring(0, 6));
		authenticateAsTeacher(fixtureA.teacherUserId());
		LeaveRequest request = leaveRequestService.submit(fixtureA.leaveType().getPublicId().toString(),
				LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Personal");
		String requestPublicId = request.getPublicId().toString();

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.approve(requestPublicId),
				"Tenant B must receive ResourceNotFoundException for tenant A's leave request");
	}

	@Test
	void teacherNotOwningLeaveRequest_cannotCancelIt() {
		activateTenant(tenantA);
		TeacherFixture owner = createTeacherWithLeaveType("own-" + UUID.randomUUID().toString().substring(0, 6));
		authenticateAsTeacher(owner.teacherUserId());
		LeaveRequest request = leaveRequestService.submit(owner.leaveType().getPublicId().toString(),
				LocalDate.now().plusDays(3), LocalDate.now().plusDays(4), "Personal");
		String requestPublicId = request.getPublicId().toString();

		authenticateAsTenantAdmin();
		String outsiderEmail = "sam-" + UUID.randomUUID().toString().substring(0, 6) + "@school.test";
		Teacher outsider = teacherService.hire("EMP-OUT-" + UUID.randomUUID().toString().substring(0, 6), "Sam", "Lee",
				outsiderEmail, LocalDate.of(2021, 1, 1));
		Long outsiderUserId = createUserWithRole(outsiderEmail, "TEACHER");
		outsider.setUserId(outsiderUserId);
		teacherRepository.save(outsider);

		authenticateAsTeacher(outsiderUserId);
		assertThrows(AccessDeniedException.class, () -> leaveRequestService.cancel(requestPublicId));
	}

	@Test
	void leaveRequestApproval_deductsAllocatedBalance() {
		activateTenant(tenantA);
		TeacherFixture fixtureA = createTeacherWithLeaveType("apr-" + UUID.randomUUID().toString().substring(0, 6));
		leaveBalanceService.allocateIfAbsent(fixtureA.teacher().getId(), fixtureA.leaveType().getId(),
				fixtureA.academicYear().getId(), BigDecimal.valueOf(12));

		authenticateAsTeacher(fixtureA.teacherUserId());
		LeaveRequest request = leaveRequestService.submit(fixtureA.leaveType().getPublicId().toString(),
				LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), "Personal");

		authenticateAsTenantAdmin();
		LeaveRequest approved = leaveRequestService.approve(request.getPublicId().toString());

		assertEquals(LeaveRequestStatus.APPROVED, approved.getStatus());
		List<LeaveBalance> balances = leaveBalanceService.listForEmployee(
				fixtureA.teacher().getPublicId().toString(), fixtureA.academicYear().getPublicId().toString());
		assertEquals(0, BigDecimal.valueOf(9).compareTo(balances.get(0).remainingDays()));
	}
}
