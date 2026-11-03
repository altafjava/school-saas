package com.altafjava.school.application.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.platform.domain.scheduler.model.TriggerType;
import com.altafjava.school.application.alert.FeeOverdueReminderRuleEvaluator;
import com.altafjava.school.application.alert.FeePaymentReminderRuleEvaluator;

/** The date rules themselves are tested in the two fee reminder evaluator tests. */
@ExtendWith(MockitoExtension.class)
class FeePaymentReminderJobTest {

	@Mock
	private AlertDispatchService alertDispatchService;

	private FeePaymentReminderJob job;

	@BeforeEach
	void setUp() {
		job = new FeePaymentReminderJob(alertDispatchService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private JobExecutionContext context() {
		return new JobExecutionContext(UUID.randomUUID(), UUID.randomUUID(), "FeePaymentReminder", "school",
				TriggerType.SCHEDULED, null, Instant.now(), null);
	}

	@Test
	void execute_dispatchesTheDueAndOverdueRules() {
		when(alertDispatchService.evaluateAndDispatch(1L, FeePaymentReminderRuleEvaluator.RULE_TYPE)).thenReturn(2);
		when(alertDispatchService.evaluateAndDispatch(1L, FeeOverdueReminderRuleEvaluator.RULE_TYPE)).thenReturn(1);

		JobExecutionResult result = job.execute(context());

		verify(alertDispatchService).evaluateAndDispatch(1L, FeePaymentReminderRuleEvaluator.RULE_TYPE);
		verify(alertDispatchService).evaluateAndDispatch(1L, FeeOverdueReminderRuleEvaluator.RULE_TYPE);
		assertEquals(new JobExecutionResult.Success(Map.of("dueCount", 2, "overdueCount", 1), null), result);
	}
}
