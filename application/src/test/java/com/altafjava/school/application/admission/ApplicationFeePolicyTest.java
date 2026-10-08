package com.altafjava.school.application.admission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.core.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ApplicationFeePolicyTest {

	@Mock
	private TenantSettingOverrideService settings;

	private ApplicationFeePolicy policy() {
		return new ApplicationFeePolicy(settings);
	}

	@Test
	void feeFor_returnsTheConfiguredAmount() {
		when(settings.get(1L, ApplicationFeePolicy.SETTING_KEY)).thenReturn(Optional.of("250.50"));

		assertEquals(0, new BigDecimal("250.50").compareTo(policy().feeFor(1L).orElseThrow()));
	}

	@Test
	void feeFor_treatsAbsentCorruptAndNonPositiveValuesAsNoFee() {
		when(settings.get(1L, ApplicationFeePolicy.SETTING_KEY)).thenReturn(Optional.empty());
		assertTrue(policy().feeFor(1L).isEmpty());
		when(settings.get(1L, ApplicationFeePolicy.SETTING_KEY)).thenReturn(Optional.of("not-a-number"));
		assertTrue(policy().feeFor(1L).isEmpty());
		when(settings.get(1L, ApplicationFeePolicy.SETTING_KEY)).thenReturn(Optional.of("-5"));
		assertTrue(policy().feeFor(1L).isEmpty());
	}

	@Test
	void set_storesThePlainAmount() {
		policy().set(1L, new BigDecimal("300.00"));

		verify(settings).set(1L, ApplicationFeePolicy.SETTING_KEY, "300.00");
	}

	@Test
	void set_zeroOrNullRemovesTheFee() {
		policy().set(1L, BigDecimal.ZERO);
		policy().set(1L, null);

		verify(settings, org.mockito.Mockito.times(2)).remove(1L, ApplicationFeePolicy.SETTING_KEY);
		verify(settings, never()).set(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void set_rejectsNegativeAndSubCentAmounts() {
		assertThrows(BusinessException.class, () -> policy().set(1L, new BigDecimal("-1")));
		assertThrows(BusinessException.class, () -> policy().set(1L, new BigDecimal("1.005")));
	}
}
