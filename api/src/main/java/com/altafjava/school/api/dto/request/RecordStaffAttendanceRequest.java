package com.altafjava.school.api.dto.request;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.staffattendance.model.StaffAttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Records or corrects a day. The times are optional, and must fall on the day being recorded.")
public record RecordStaffAttendanceRequest(@NotNull StaffAttendanceStatus status, LocalDateTime checkInAt,
		LocalDateTime checkOutAt, @Size(max = 500) String note) {
}
