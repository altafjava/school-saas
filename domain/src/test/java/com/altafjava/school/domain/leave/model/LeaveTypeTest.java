package com.altafjava.school.domain.leave.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class LeaveTypeTest {

	@Test
	void create_isActiveByDefault() {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));

		assertTrue(leaveType.isActive());
		assertEquals("Sick Leave", leaveType.getName());
	}

	@Test
	void create_isPaidByDefault() {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));

		assertTrue(leaveType.isPaid());
	}

	@Test
	void markUnpaid_flipsPaidFlag() {
		LeaveType leaveType = LeaveType.create("Unpaid Leave", BigDecimal.valueOf(0));

		leaveType.markUnpaid();

		assertFalse(leaveType.isPaid());
	}

	@Test
	void markPaid_reversesMarkUnpaid() {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));
		leaveType.markUnpaid();

		leaveType.markPaid();

		assertTrue(leaveType.isPaid());
	}

	@Test
	void updateDetails_replacesMutableFields() {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));

		leaveType.updateDetails("Medical Leave", BigDecimal.valueOf(15));

		assertEquals("Medical Leave", leaveType.getName());
		assertEquals(0, BigDecimal.valueOf(15).compareTo(leaveType.getDefaultAnnualDays()));
	}

	@Test
	void deactivate_flipsActiveFlag() {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));

		leaveType.deactivate();

		assertFalse(leaveType.isActive());
	}

	@Test
	void create_needsOnlyAnAdministratorsApproval() {
		LeaveType leaveType = LeaveType.create("Sick Leave", BigDecimal.valueOf(12));

		assertEquals(1, leaveType.getApprovalLevels());
		assertFalse(leaveType.requiresDepartmentHeadApproval());
	}

	@Test
	void configureApprovalLevels_two_requiresTheDepartmentHead() {
		LeaveType leaveType = LeaveType.create("Casual", BigDecimal.valueOf(8));

		leaveType.configureApprovalLevels(2);

		assertTrue(leaveType.requiresDepartmentHeadApproval());
	}

	@Test
	void configureApprovalLevels_outsideOneToTwo_throwsBusinessException() {
		LeaveType leaveType = LeaveType.create("Casual", BigDecimal.valueOf(8));

		assertThrows(BusinessException.class, () -> leaveType.configureApprovalLevels(0));
		assertThrows(BusinessException.class, () -> leaveType.configureApprovalLevels(3));
	}
}
