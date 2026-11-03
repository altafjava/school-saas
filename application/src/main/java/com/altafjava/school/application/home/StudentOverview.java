package com.altafjava.school.application.home;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

/**
 * What a family needs to know about one student. A card the caller's role may not read is absent,
 * so a tenant that withholds something from a role withholds it here too.
 */
public record StudentOverview(ReferenceSummary student, ReferenceSummary classroom, Attendance attendance, Fees fees,
		List<UpcomingExam> upcomingExams, List<RecentResult> recentResults, List<Homework> homework,
		List<LeaveRequest> leaveRequests) {

	public StudentOverview {
		upcomingExams = upcomingExams == null ? null : List.copyOf(upcomingExams);
		recentResults = recentResults == null ? null : List.copyOf(recentResults);
		homework = homework == null ? null : List.copyOf(homework);
		leaveRequests = leaveRequests == null ? null : List.copyOf(leaveRequests);
	}

	/** {@code today} is absent until the register is marked; the percentage covers the year so far. */
	public record Attendance(AttendanceStatus today, long presentDays, long markedDays, BigDecimal percentage) {
	}

	/** {@code nextDueDate} is absent when nothing is owed or the charge has no due date. */
	public record Fees(BigDecimal outstanding, LocalDate nextDueDate, boolean overdue) {
	}

	public record UpcomingExam(String examPublicId, String title, ReferenceSummary subject,
			LocalDateTime scheduledAt) {
	}

	public record RecentResult(String examPublicId, String examTitle, ReferenceSummary subject, BigDecimal marks,
			BigDecimal maxMarks, String gradeLetter, Instant publishedAt) {
	}

	/** Not yet handed in; a due date in the past means it is overdue. */
	public record Homework(String assignmentPublicId, String title, ReferenceSummary subject, LocalDate dueDate) {
	}

	public record LeaveRequest(String leaveRequestPublicId, LocalDate startDate, LocalDate endDate, String reason) {
	}
}
