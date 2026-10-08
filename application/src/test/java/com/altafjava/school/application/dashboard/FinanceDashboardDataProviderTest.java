package com.altafjava.school.application.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeRefundRepository;
import com.altafjava.school.domain.library.repository.CirculationRepository;

@ExtendWith(MockitoExtension.class)
class FinanceDashboardDataProviderTest {

	@Mock
	private FeePaymentRepository feePaymentRepository;
	@Mock
	private CirculationRepository circulationRepository;
	@Mock
	private FeeRefundRepository feeRefundRepository;

	private FinanceDashboardDataProvider provider;

	@BeforeEach
	void setUp() {
		provider = new FinanceDashboardDataProvider(feePaymentRepository, circulationRepository, feeRefundRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void fetchData_returnsSingleSummaryRow() {
		when(feePaymentRepository.sumPaidAmountByTenantId(1L)).thenReturn(BigDecimal.valueOf(50000));
		when(feeRefundRepository.sumCompletedByTenantId(1L)).thenReturn(BigDecimal.valueOf(2000));
		when(feePaymentRepository.countByTenantId(1L)).thenReturn(200L);
		when(circulationRepository.sumFineAmountByTenantId(1L)).thenReturn(BigDecimal.valueOf(150));

		List<Map<String, Object>> result = provider.fetchData(Map.of());

		assertEquals(1, result.size());
		Map<String, Object> row = result.get(0);
		assertEquals(0, BigDecimal.valueOf(50000).compareTo((BigDecimal) row.get("totalFeeCollected")));
		assertEquals(0, BigDecimal.valueOf(2000).compareTo((BigDecimal) row.get("totalFeeRefunded")));
		assertEquals(0, BigDecimal.valueOf(48000).compareTo((BigDecimal) row.get("netFeeCollected")));
		assertEquals(200L, row.get("feePaymentCount"));
		assertEquals(0, BigDecimal.valueOf(150).compareTo((BigDecimal) row.get("totalLibraryFinesCollected")));
	}
}
