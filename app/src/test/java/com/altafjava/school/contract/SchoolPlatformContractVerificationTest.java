package com.altafjava.school.contract;

import static org.assertj.core.api.Assertions.assertThat;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import com.altafjava.platform.application.event.events.TenantCreatedEvent;
import com.altafjava.platform.core.PlatformConfigurer;
import com.altafjava.school.application.listener.SchoolTenantProvisioningListener;
import com.altafjava.school.application.privacy.SchoolDataRetentionHandler;
import com.altafjava.school.application.privacy.StudentGuardianPiiHandler;
import com.altafjava.school.application.sync.AttendanceOfflineSyncHandler;
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
		// None of these tests touch offlineSyncEntityHandlers/the injected handler bean.
		// Map.of rejects a null value, so the constructor needs a real (mock) handler even
		// though none of these tests touch offlineSyncEntityHandlers() itself.
		SchoolPlatformConfigurer configurer = new SchoolPlatformConfigurer(
				Mockito.mock(AttendanceOfflineSyncHandler.class), Mockito.mock(StudentGuardianPiiHandler.class),
				Mockito.mock(SchoolDataRetentionHandler.class));
		assertThat(configurer.platformName())
				.as("SchoolPlatformConfigurer.platformName must return a non-blank platform name")
				.isNotBlank();
	}

	@Test
	@DisplayName("SchoolPlatformConfigurer.domainTenantChangelogPaths returns at least one changelog")
	void schoolPlatformConfigurer_domainTenantChangelogPaths_atLeastOne() {
		// None of these tests touch offlineSyncEntityHandlers/the injected handler bean.
		// Map.of rejects a null value, so the constructor needs a real (mock) handler even
		// though none of these tests touch offlineSyncEntityHandlers() itself.
		SchoolPlatformConfigurer configurer = new SchoolPlatformConfigurer(
				Mockito.mock(AttendanceOfflineSyncHandler.class), Mockito.mock(StudentGuardianPiiHandler.class),
				Mockito.mock(SchoolDataRetentionHandler.class));
		List<String> paths = configurer.domainTenantChangelogPaths();
		assertThat(paths)
				.as("School domain must register at least one Liquibase changelog for tenant schema seeding")
				.isNotEmpty();
	}

	@Test
	@DisplayName("SchoolPlatformConfigurer.maxTenantsPerInstance returns a positive value")
	void schoolPlatformConfigurer_maxTenantsPerInstance_isPositive() {
		// None of these tests touch offlineSyncEntityHandlers/the injected handler bean.
		// Map.of rejects a null value, so the constructor needs a real (mock) handler even
		// though none of these tests touch offlineSyncEntityHandlers() itself.
		SchoolPlatformConfigurer configurer = new SchoolPlatformConfigurer(
				Mockito.mock(AttendanceOfflineSyncHandler.class), Mockito.mock(StudentGuardianPiiHandler.class),
				Mockito.mock(SchoolDataRetentionHandler.class));
		assertThat(configurer.maxTenantsPerInstance())
				.as("maxTenantsPerInstance must be a positive integer")
				.isPositive();
	}

	@Test
	@DisplayName("SchoolPlatformConfigurer.domainPiiHandler is present")
	void schoolPlatformConfigurer_domainPiiHandler_isPresent() {
		StudentGuardianPiiHandler piiHandler = Mockito.mock(StudentGuardianPiiHandler.class);
		SchoolPlatformConfigurer configurer = new SchoolPlatformConfigurer(
				Mockito.mock(AttendanceOfflineSyncHandler.class), piiHandler,
				Mockito.mock(SchoolDataRetentionHandler.class));
		assertThat(configurer.domainPiiHandler())
				.as("school-saas holds Student/Guardian PII, so a GDPR/DPDP erasure request must "
						+ "reach it — an empty domainPiiHandler() would silently skip domain-side erasure")
				.contains(piiHandler);
	}

	@Test
	@DisplayName("SchoolPlatformConfigurer.domainRetentionHandler is present")
	void schoolPlatformConfigurer_domainRetentionHandler_isPresent() {
		SchoolDataRetentionHandler retentionHandler = Mockito.mock(SchoolDataRetentionHandler.class);
		SchoolPlatformConfigurer configurer = new SchoolPlatformConfigurer(
				Mockito.mock(AttendanceOfflineSyncHandler.class), Mockito.mock(StudentGuardianPiiHandler.class),
				retentionHandler);
		assertThat(configurer.domainRetentionHandler())
				.as("school-saas holds Student PII subject to FERPA/COPPA retention windows, so the daily "
						+ "retention sweep must reach it — an empty domainRetentionHandler() would silently skip "
						+ "domain-side retention enforcement")
				.contains(retentionHandler);
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
}
