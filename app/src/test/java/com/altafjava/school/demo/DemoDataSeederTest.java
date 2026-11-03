package com.altafjava.school.demo;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import com.altafjava.platform.application.service.FeatureFlagService;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ExamTypeDefinitionService;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoDataSeederTest {

	private final Tenant tenant = Tenant.builder().id(1L).subdomain(DemoDataSeeder.SUBDOMAIN).type(TenantType.SHARED)
			.build();

	@Mock
	private TenantRepository tenantRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private AcademicYearService academicYearService;
	@Mock
	private ExamTypeDefinitionService examTypeDefinitionService;
	@Mock
	private FeatureFlagService featureFlagService;
	@Mock
	private PlatformTransactionManager transactionManager;
	@Mock
	private TransactionStatus transaction;
	@Mock
	private SchoolClock schoolClock;

	@InjectMocks
	private DemoDataSeeder seeder;

	@BeforeEach
	void demoTenantExists() {
		when(tenantRepository.findBySubdomain(DemoDataSeeder.SUBDOMAIN)).thenReturn(Optional.of(tenant));
		when(transactionManager.getTransaction(any())).thenReturn(transaction);
		when(examTypeDefinitionService.listActive()).thenReturn(List.of(mock(ExamTypeDefinition.class)));
		when(schoolClock.today()).thenReturn(LocalDate.of(2026, 6, 1));
		when(schoolClock.now()).thenReturn(LocalDateTime.of(2026, 6, 1, 9, 0));
	}

	@Test
	void seed_alreadyFinished_doesNothing() {
		when(userRepository.existsByEmail("parent@demo.school")).thenReturn(true);

		assertSame(tenant, seeder.seed());

		verify(transactionManager, never()).getTransaction(any());
	}

	@Test
	void seed_waitsForProvisioning_beforeOpeningItsTransaction() {
		when(examTypeDefinitionService.listActive()).thenReturn(List.of())
				.thenReturn(List.of(mock(ExamTypeDefinition.class)));
		when(academicYearService.create(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
				.thenThrow(new IllegalStateException("stop after the transaction opens"));

		assertThrows(IllegalStateException.class, seeder::seed);

		InOrder order = inOrder(examTypeDefinitionService, transactionManager);
		order.verify(examTypeDefinitionService, times(2)).listActive();
		order.verify(transactionManager).getTransaction(any());
	}

	@Test
	void seed_failingPartWay_rollsBackAndLeavesTheRetryMarkerUnset() {
		when(academicYearService.create(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
				.thenThrow(new IllegalStateException("seed failed"));

		assertThrows(IllegalStateException.class, seeder::seed);

		verify(transactionManager).rollback(transaction);
		verify(transactionManager, never()).commit(transaction);
	}
}
