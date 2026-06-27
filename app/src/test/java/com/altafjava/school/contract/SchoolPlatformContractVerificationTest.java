package com.altafjava.school.contract;

import static org.assertj.core.api.Assertions.assertThat;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import com.altafjava.platform.application.event.events.TenantCreatedEvent;
import com.altafjava.platform.core.PlatformConfigurer;
import com.altafjava.school.application.listener.SchoolTenantProvisioningListener;
import com.altafjava.school.config.SchoolPlatformConfigurer;

/**
 * Consumer-side contract verification tests.
 *
 * <p>
 * Verifies that school-saas correctly implements every platform extension point defined in the
 * platform's published contracts (see {@code PlatformExtensionContractTest} in platform-saas).
 * These tests catch regressions where a platform upgrade adds a new required contract element that
 * the school consumer has not yet implemented, or where school removes a required implementation.
 *
 * <p>
 * Contracts verified:
 * <ol>
 * <li>{@link SchoolPlatformConfigurer} correctly implements {@link PlatformConfigurer}.</li>
 * <li>{@link SchoolTenantProvisioningListener} handles {@link TenantCreatedEvent} with the
 * required transactional and async guarantees.</li>
 * </ol>
 */
@DisplayName("School-saas Platform Contract Verification")
class SchoolPlatformContractVerificationTest {

	@Test
	@DisplayName("SchoolPlatformConfigurer implements PlatformConfigurer")
	void schoolPlatformConfigurer_implementsPlatformConfigurer() {
		assertThat(PlatformConfigurer.class)
				.as("SchoolPlatformConfigurer must implement PlatformConfigurer")
				.isAssignableFrom(SchoolPlatformConfigurer.class);
	}

	@Test
	@DisplayName("SchoolPlatformConfigurer.platformName returns a non-blank value")
	void schoolPlatformConfigurer_platformName_isNonBlank() {
		SchoolPlatformConfigurer configurer = new SchoolPlatformConfigurer();
		assertThat(configurer.platformName())
				.as("SchoolPlatformConfigurer.platformName must return a non-blank platform name")
				.isNotBlank();
	}

	@Test
	@DisplayName("SchoolPlatformConfigurer.domainTenantChangelogPaths returns at least one changelog")
	void schoolPlatformConfigurer_domainTenantChangelogPaths_atLeastOne() {
		SchoolPlatformConfigurer configurer = new SchoolPlatformConfigurer();
		List<String> paths = configurer.domainTenantChangelogPaths();
		assertThat(paths)
				.as("School domain must register at least one Liquibase changelog for tenant schema seeding")
				.isNotEmpty();
	}

	@Test
	@DisplayName("SchoolPlatformConfigurer.maxTenantsPerInstance returns a positive value")
	void schoolPlatformConfigurer_maxTenantsPerInstance_isPositive() {
		SchoolPlatformConfigurer configurer = new SchoolPlatformConfigurer();
		assertThat(configurer.maxTenantsPerInstance())
				.as("maxTenantsPerInstance must be a positive integer")
				.isPositive();
	}

	@Test
	@DisplayName("SchoolTenantProvisioningListener has a method listening to TenantCreatedEvent")
	void schoolTenantProvisioningListener_hasOnTenantCreatedHandler() {
		List<Method> handlers = Arrays.stream(SchoolTenantProvisioningListener.class.getMethods())
				.filter(m -> {
					boolean paramMatch = Arrays.stream(m.getParameterTypes())
							.anyMatch(p -> p.isAssignableFrom(TenantCreatedEvent.class));
					boolean hasEventListener = m.isAnnotationPresent(EventListener.class)
							|| m.isAnnotationPresent(TransactionalEventListener.class);
					return paramMatch && hasEventListener;
				})
				.toList();

		assertThat(handlers)
				.as("SchoolTenantProvisioningListener must have exactly one handler for TenantCreatedEvent "
						+ "annotated with @EventListener or @TransactionalEventListener")
				.hasSize(1);
	}

	@Test
	@DisplayName("SchoolTenantProvisioningListener TenantCreatedEvent handler is @Async")
	void schoolTenantProvisioningListener_onTenantCreated_isAsync() {
		List<Method> asyncHandlers = Arrays.stream(SchoolTenantProvisioningListener.class.getMethods())
				.filter(m -> {
					boolean paramMatch = Arrays.stream(m.getParameterTypes())
							.anyMatch(p -> p.isAssignableFrom(TenantCreatedEvent.class));
					boolean hasEventListener = m.isAnnotationPresent(EventListener.class)
							|| m.isAnnotationPresent(TransactionalEventListener.class);
					boolean isAsync = m.isAnnotationPresent(Async.class);
					return paramMatch && hasEventListener && isAsync;
				})
				.toList();

		assertThat(asyncHandlers)
				.as("SchoolTenantProvisioningListener.onTenantCreated must be @Async — "
						+ "synchronous tenant provisioning blocks the request thread")
				.hasSize(1);
	}

	@Test
	@DisplayName("SchoolPlatformConfigurer.accessTokenExpiry does not exceed 24 hours")
	void schoolPlatformConfigurer_accessTokenExpiry_isReasonable() {
		SchoolPlatformConfigurer configurer = new SchoolPlatformConfigurer();
		assertThat(configurer.accessTokenExpiry().toHours())
				.as("Access token expiry should not exceed 24 hours for security reasons")
				.isLessThanOrEqualTo(24);
	}
}
