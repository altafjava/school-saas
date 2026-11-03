package com.altafjava.school.application.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.time.TestSchoolClocks;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;

@ExtendWith(MockitoExtension.class)
class FeeCollectionTrendDataProviderTest {

	@Mock
	private FeePaymentRepository feePaymentRepository;

	private FeeCollectionTrendDataProvider provider;

	@BeforeEach
	void setUp() {
		provider = new FeeCollectionTrendDataProvider(feePaymentRepository, TestSchoolClocks.systemDefault());
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void fetchData_defaultPeriods_returnsSixMonthlyRows() {
		when(feePaymentRepository.sumClearedAmountByTenantIdAndPaidAtBetween(eq(1L), any(), any()))
				.thenReturn(BigDecimal.valueOf(5000));

		List<Map<String, Object>> rows = provider.fetchData(Map.of());

		assertEquals(6, rows.size());
		assertEquals(BigDecimal.valueOf(5000), rows.get(0).get("totalCollected"));
	}

	@Test
	void fetchData_explicitPeriods_returnsThatManyRows() {
		when(feePaymentRepository.sumClearedAmountByTenantIdAndPaidAtBetween(eq(1L), any(), any()))
				.thenReturn(BigDecimal.ZERO);

		List<Map<String, Object>> rows = provider.fetchData(Map.of("periods", 2));

		assertEquals(2, rows.size());
	}
}
