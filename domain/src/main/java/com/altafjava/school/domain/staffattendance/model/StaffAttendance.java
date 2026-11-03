package com.altafjava.school.domain.staffattendance.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** One staff member's day: when they arrived and left, and what that made the day. One row per day. */
@Entity
@Table(name = "staff_attendance")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class StaffAttendance extends SoftDeletableEntity {

	// FK to employees.id
	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	@Column(name = "attendance_date", nullable = false)
	private LocalDate attendanceDate;

	// Times are the school's local time, like the attendance date.
	@Column(name = "check_in_at")
	private LocalDateTime checkInAt;

	@Column(name = "check_out_at")
	private LocalDateTime checkOutAt;

	@Column(name = "late", nullable = false)
	private boolean late;

	@Column(name = "left_early", nullable = false)
	private boolean leftEarly;

	@Column(name = "note", length = 500)
	private String note;

	@Column(name = "marked_by", length = 100)
	private String markedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StaffAttendanceStatus status;

	@PrePersist
	@PreUpdate
	private void assertConsistent() {
		if (checkOutAt != null && (checkInAt == null || checkOutAt.isBefore(checkInAt))) {
			throw new BusinessException("A check-out needs an earlier check-in");
		}
		if (status == StaffAttendanceStatus.ABSENT && checkInAt != null) {
			throw new BusinessException("An absent day has no check-in");
		}
	}

	public static StaffAttendance checkIn(Long employeeId, LocalDateTime at, StaffWorkday workday) {
		return StaffAttendance.builder()
				.employeeId(employeeId)
				.attendanceDate(at.toLocalDate())
				.checkInAt(at)
				.late(workday.isLate(at.toLocalTime()))
				.markedBy("self")
				.status(StaffAttendanceStatus.PRESENT)
				.build();
	}

	/** A day recorded by an administrator, for someone who could not check in or out themselves. */
	public static StaffAttendance recorded(Long employeeId, LocalDate date, DayRecord record, StaffWorkday workday,
			String markedBy) {
		StaffAttendance day = StaffAttendance.builder().employeeId(employeeId).attendanceDate(date).build();
		day.correct(record, workday, markedBy);
		return day;
	}

	public void checkOut(LocalDateTime at, StaffWorkday workday) {
		if (checkInAt == null) {
			throw new BusinessException("Check in before checking out");
		}
		if (checkOutAt != null) {
			throw new BusinessException("Already checked out today");
		}
		if (!at.isAfter(checkInAt)) {
			throw new BusinessException("Check-out must come after check-in");
		}
		this.checkOutAt = at;
		applyDeparture(workday);
	}

	/** Replaces the day as an administrator records it; the times decide late, early and half day. */
	public void correct(DayRecord record, StaffWorkday workday, String markedBy) {
		record.requireConsistentWith(attendanceDate);
		this.checkInAt = record.checkInAt();
		this.checkOutAt = record.checkOutAt();
		this.note = record.note();
		this.markedBy = markedBy;
		this.late = checkInAt != null && workday.isLate(checkInAt.toLocalTime());
		this.leftEarly = false;
		this.status = record.status();
		if (checkOutAt != null) {
			applyDeparture(workday);
		}
	}

	private void applyDeparture(StaffWorkday workday) {
		this.leftEarly = workday.isEarly(checkOutAt.toLocalTime());
		if (workedMinutes() < workday.halfDayMinutes()) {
			this.status = StaffAttendanceStatus.HALF_DAY;
		}
	}

	public long workedMinutes() {
		return checkInAt == null || checkOutAt == null ? 0 : Duration.between(checkInAt, checkOutAt).toMinutes();
	}

	/** What an administrator records for a day. Times are optional, but must fall on that day. */
	public record DayRecord(StaffAttendanceStatus status, LocalDateTime checkInAt, LocalDateTime checkOutAt,
			String note) {

		void requireConsistentWith(LocalDate date) {
			if (status == null) {
				throw new BusinessException("A status is required");
			}
			if (checkInAt != null && !checkInAt.toLocalDate().equals(date)
					|| checkOutAt != null && !checkOutAt.toLocalDate().equals(date)) {
				throw new BusinessException("Check-in and check-out must fall on " + date);
			}
			if (status == StaffAttendanceStatus.ABSENT && (checkInAt != null || checkOutAt != null)) {
				throw new BusinessException("An absent day has no check-in or check-out");
			}
		}
	}
}
