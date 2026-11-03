package com.altafjava.school.api.dto.response.staffattendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.staffattendance.model.StaffAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One staff member's recorded day. Times are the school's local time.")
public record StaffAttendanceResponse(String publicId, Long version, String employeePublicId,
		ReferenceSummary employee, LocalDate date, StaffAttendanceStatus status, LocalDateTime checkInAt,
		LocalDateTime checkOutAt, boolean late, boolean leftEarly, long workedMinutes, String note) {
}
