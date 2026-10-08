package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.security.PermissionCatalogService;
import com.altafjava.platform.core.security.permission.PermissionDefinition;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.user.model.Role;
import com.altafjava.platform.domain.user.repository.RoleRepository;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;

/** The Liquibase-seeded role templates must only grant codes the permission catalog knows. */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class SeededRolePermissionsIntegrationTest extends SchoolIntegrationTestBase {

	private static final Set<String> SCHOOL_TEMPLATES = Set.of("TENANT_ADMIN", "TEACHER", "STUDENT", "PARENT",
			"PRINCIPAL", "FINANCE", "HR", "ACADEMIC");

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PermissionCatalogService permissionCatalogService;

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void everySeededTemplate_grantsOnlyCataloguedPermissions() {
		Set<String> catalogue = permissionCatalogService.all().stream().map(PermissionDefinition::code)
				.collect(Collectors.toSet());

		for (Role template : roleRepository.findAllGlobalTemplates()) {
			Set<String> unknown = template.permissionCodes().stream().filter(code -> !catalogue.contains(code))
					.collect(Collectors.toSet());
			assertTrue(unknown.isEmpty(), template.getName() + " seeds uncatalogued permissions " + unknown);
		}
	}

	@Test
	void everySchoolTemplate_isSeededWithPermissions() {
		Set<String> seeded = roleRepository.findAllGlobalTemplates().stream()
				.filter(role -> !role.permissionCodes().isEmpty()).map(Role::getName).collect(Collectors.toSet());

		assertEquals(SCHOOL_TEMPLATES, seeded);
	}
}
