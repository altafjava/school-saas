package com.altafjava.school.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.report.model.ReportDefinition;
import com.altafjava.platform.domain.report.model.ReportType;
import com.altafjava.platform.domain.report.repository.ReportDefinitionRepository;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.dashboard.HrDashboardDataProvider;
import com.altafjava.school.application.dashboard.PrincipalDashboardDataProvider;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;

/**
 * Verifies each role-scoped dashboard's aggregate reflects only the current tenant's data, and
 * that every tenant gets its four dashboard {@code ReportDefinition}s seeded for export.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class DashboardTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	private static final Set<String> EXPECTED_DASHBOARD_REPORT_NAMES = Set.of("Principal Dashboard",
			"Finance Dashboard", "HR Dashboard", "Academic Dashboard");

	@Autowired
	private PrincipalDashboardDataProvider principalDashboardDataProvider;

	@Autowired
	private HrDashboardDataProvider hrDashboardDataProvider;

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private ReportDefinitionRepository reportDefinitionRepository;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "dash-a-" + suffix, 1L, "admin@dash-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "dash-b-" + suffix, 1L, "admin@dash-b.test", "Password123!", "USD"));
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

	@Test
	void hrDashboard_reflectsOnlyCurrentTenantTeacherCount() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		String suffix = UUID.randomUUID().toString().substring(0, 6);
		teacherService.hire("EMP-" + suffix, "Jane", "Doe", "jane-" + suffix + "@school.test",
				LocalDate.of(2020, 1, 1));

		Long tenantATeacherCount = (Long) hrDashboardDataProvider.fetchData(Map.of()).get(0).get("teacherCount");
		assertTrue(tenantATeacherCount >= 1, "Tenant A must see its own teacher");

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		Long tenantBTeacherCount = (Long) hrDashboardDataProvider.fetchData(Map.of()).get(0).get("teacherCount");

		assertEquals(0L, tenantBTeacherCount, "Tenant B must not see tenant A's teacher in its own dashboard");
	}

	@Test
	void principalDashboard_returnsSingleSummaryRow() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();

		List<Map<String, Object>> result = principalDashboardDataProvider.fetchData(Map.of());

		assertEquals(1, result.size());
		assertTrue(result.get(0).containsKey("activeStudentCount"));
		assertTrue(result.get(0).containsKey("attendancePercentageLast30Days"));
		assertTrue(result.get(0).containsKey("upcomingEventCount"));
	}

	@Test
	void everyNewTenant_getsFourDashboardReportDefinitionsSeeded() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();

		// Seeding happens inside the @Async TenantCreatedEvent listener — poll rather than assert
		// immediately, since registerTenant() in @BeforeEach returns before that listener runs.
		// Poll on this thread: the tenant context is thread-bound, and another thread would read every tenant's rows.
		await().atMost(Duration.ofSeconds(5)).pollInSameThread().untilAsserted(() -> {
			Set<String> seededNames = currentTenantDashboardReportNames();
			assertTrue(seededNames.containsAll(EXPECTED_DASHBOARD_REPORT_NAMES),
					"Expected all four dashboard report definitions to be seeded, found: " + seededNames);
		});

		List<ReportDefinition> definitions = reportDefinitionRepository
				.findAll(com.altafjava.platform.core.model.Pageable.of(0, 20)).content();
		assertTrue(definitions.stream()
				.filter(d -> EXPECTED_DASHBOARD_REPORT_NAMES.contains(d.getName()))
				.allMatch(d -> d.getType() == ReportType.SERVICE_CALL));
	}

	private Set<String> currentTenantDashboardReportNames() {
		return reportDefinitionRepository.findAll(com.altafjava.platform.core.model.Pageable.of(0, 20)).content()
				.stream()
				.map(ReportDefinition::getName)
				.collect(Collectors.toSet());
	}
}
