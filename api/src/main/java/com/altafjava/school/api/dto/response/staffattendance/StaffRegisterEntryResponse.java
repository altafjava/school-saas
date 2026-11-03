package com.altafjava.school.api.dto.response.staffattendance;

import java.time.LocalDateTime;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.application.staffattendance.RegisterStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One employee's line on the day's register. NOT_MARKED means nothing is recorded yet.")
public record StaffRegisterEntryResponse(String employeePublicId, ReferenceSummary employee, RegisterStatus status,
		@NoLabel("The recorded day itself; its fields are on this line") String attendancePublicId,
		LocalDateTime checkInAt, LocalDateTime checkOutAt, boolean late, boolean leftEarly,
		@NoLabel("Approved leave covering the day; the status says so") String leaveRequestPublicId) {
}
