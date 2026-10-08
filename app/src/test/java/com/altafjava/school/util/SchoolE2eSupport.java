package com.altafjava.school.util;

import static io.restassured.RestAssured.given;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.security.PasswordEncoder;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserStatus;
import com.altafjava.platform.domain.user.repository.RoleRepository;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Shared scaffolding for HTTP-level tests: provisions a school (tenant + admin), mints requests
 * scoped to it, and creates the handful of records almost every scenario needs through the real API.
 */
@Component
public class SchoolE2eSupport {

	public static final String PASSWORD = "Password123!";

	public record School(Long tenantId, String adminEmail, String adminToken) {
	}

	private final TenantOnboardingService onboardingService;
	private final SchoolAuthenticationHelper authHelper;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AcademicYearService academicYearService;
	private final AcademicYearRepository academicYearRepository;
	private final EmployeeRepository employeeRepository;

	public SchoolE2eSupport(TenantOnboardingService onboardingService, SchoolAuthenticationHelper authHelper,
			UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
			AcademicYearService academicYearService, AcademicYearRepository academicYearRepository,
			EmployeeRepository employeeRepository) {
		this.onboardingService = onboardingService;
		this.authHelper = authHelper;
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.academicYearService = academicYearService;
		this.academicYearRepository = academicYearRepository;
		this.employeeRepository = employeeRepository;
	}

	public School provisionSchool(String name) {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String adminEmail = "admin-" + suffix + "@school.test";
		Tenant tenant = onboardingService.registerTenant(
				new RegisterTenantCommand(name, "e2e-" + suffix, 1L, adminEmail, PASSWORD, "USD"));
		return new School(tenant.getId(), adminEmail, login(tenant.getId(), adminEmail, PASSWORD));
	}

	public RequestSpecification request(School school) {
		return request(school.tenantId(), school.adminToken());
	}

	public RequestSpecification request(Long tenantId, String token) {
		return given().header("X-Tenant-ID", tenantId).header("Authorization", "Bearer " + token)
				.contentType(ContentType.JSON);
	}

	public RequestSpecification requestWithRole(Long tenantId, String role) {
		return request(tenantId, authHelper.tokenWithRole(tenantId, role));
	}

	public RequestSpecification requestAsUser(Long tenantId, Long userId, String role) {
		return request(tenantId, authHelper.tokenForUser(tenantId, userId, "user-" + userId + "@school.test", role));
	}

	public <T> T inTenant(Long tenantId, Supplier<T> action) {
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			return action.get();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	public void inTenant(Long tenantId, Runnable action) {
		inTenant(tenantId, () -> {
			action.run();
			return null;
		});
	}

	// Global (tenant-less) roles such as STUDENT/PARENT/TEACHER are templates every tenant reuses.
	public Long createUserWithRole(Long tenantId, String email, String roleName) {
		return inTenant(tenantId, () -> {
			var role = roleRepository.findAll().stream()
					.filter(r -> r.getTenantId() == null && roleName.equals(r.getName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));
			User user = User.builder()
					.email(email)
					.passwordHash(passwordEncoder.encode(PASSWORD))
					.status(UserStatus.ACTIVE)
					.emailVerified(true)
					.build();
			user.addRole(role);
			return userRepository.save(user).getId();
		});
	}

	public String createStudent(School school) {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		return request(school)
				.body("{\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"email\":\"alice-" + suffix
						+ "@school.test\",\"dateOfBirth\":\"2010-01-01\"}")
				.post("/api/v1/students")
				.then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	public String createSubject(School school, String code) {
		return request(school)
				.body("{\"code\":\"" + code + "\",\"name\":\"" + code + "\"}")
				.post("/api/v1/subjects")
				.then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	public String createClassroom(School school, String classCode) {
		String academicYearPublicId = inTenant(school.tenantId(), () -> academicYearService
				.create(classCode + "-" + UUID.randomUUID().toString().substring(0, 4), LocalDate.of(2025, 6, 1),
						LocalDate.of(2026, 5, 31), true)
				.getPublicId().toString());
		return request(school)
				.body("{\"classCode\":\"" + classCode + "\",\"grade\":\"Grade 5\",\"section\":\"A\","
						+ "\"academicYearPublicId\":\"" + academicYearPublicId + "\"}")
				.post("/api/v1/classrooms")
				.then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	public Long currentAcademicYearId(School school) {
		return inTenant(school.tenantId(), () -> academicYearRepository
				.findByCurrentTrueAndTenantId(school.tenantId())
				.orElseGet(() -> academicYearService.create("AY-" + UUID.randomUUID().toString().substring(0, 6),
						LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(10), true))
				.getId());
	}

	public String currentAcademicYearPublicId(School school) {
		Long id = currentAcademicYearId(school);
		return inTenant(school.tenantId(), () -> academicYearRepository.findByIdAndTenantId(id, school.tenantId())
				.orElseThrow().getPublicId().toString());
	}

	public String createEmployee(School school, String firstName) {
		return request(school)
				.body("{\"staffCategory\":\"SUPPORT\",\"firstName\":\"" + firstName + "\",\"lastName\":\"Staff\","
						+ "\"email\":\"" + firstName.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8)
						+ "@school.test\",\"joinDate\":\"2025-01-01\"}")
				.post("/api/v1/employees")
				.then().statusCode(HttpStatus.CREATED.value())
				.extract().path("data.publicId");
	}

	public Long employeeId(School school, String employeePublicId) {
		return inTenant(school.tenantId(), () -> employeeRepository
				.findByPublicIdAndTenantId(UUID.fromString(employeePublicId), school.tenantId()).orElseThrow()
				.getId());
	}

	public void linkEmployeeToUser(School school, String employeePublicId, Long userId) {
		inTenant(school.tenantId(), () -> {
			var employee = employeeRepository
					.findByPublicIdAndTenantId(UUID.fromString(employeePublicId), school.tenantId()).orElseThrow();
			employee.setUserId(userId);
			employeeRepository.save(employee);
		});
	}

	public String login(Long tenantId, String email, String password) {
		long deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			io.restassured.response.Response response = given().header("X-Tenant-ID", tenantId)
					.contentType(ContentType.JSON)
					.body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
					.post("/api/v1/auth/login");
			if (response.statusCode() == HttpStatus.OK.value()) {
				return response.then().extract().path("data.accessToken");
			}
			if (System.currentTimeMillis() >= deadline) {
				response.then().statusCode(HttpStatus.OK.value());
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("login interrupted", e);
			}
		}
	}
}
