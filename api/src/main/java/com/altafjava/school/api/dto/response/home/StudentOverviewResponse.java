package com.altafjava.school.api.dto.response.home;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One student's cards. A card the caller's role may not read is absent.")
public record StudentOverviewResponse(ReferenceSummary student,
		@Schema(description = "This academic year's classroom") ReferenceSummary classroom,
		Attendance attendance, Fees fees, List<UpcomingExam> upcomingExams, List<RecentResult> recentResults,
		@Schema(description = "Assignments not yet handed in; a past due date means overdue") List<Homework> homework,
		@Schema(description = "Leave requests awaiting a decision") List<LeaveRequest> leaveRequests) {

	public StudentOverviewResponse {
		upcomingExams = upcomingExams == null ? null : List.copyOf(upcomingExams);
		recentResults = recentResults == null ? null : List.copyOf(recentResults);
		homework = homework == null ? null : List.copyOf(homework);
		leaveRequests = leaveRequests == null ? null : List.copyOf(leaveRequests);
	}

	public record Attendance(@Schema(description = "Absent until today's register is marked") AttendanceStatus today,
			long presentDays, long markedDays,
			@Schema(description = "Present days as a percentage of marked days this academic year") BigDecimal percentage) {
	}

	public record Fees(BigDecimal outstanding, LocalDate nextDueDate, boolean overdue) {
	}

	public record UpcomingExam(@NoLabel("Named by the title or dates beside it") String examPublicId, String title,
			ReferenceSummary subject,
			LocalDateTime scheduledAt) {
	}

	public record RecentResult(@NoLabel("Named by the title or dates beside it") String examPublicId, String examTitle,
			ReferenceSummary subject, BigDecimal marks,
			BigDecimal maxMarks, String gradeLetter, Instant publishedAt) {
	}

	public record Homework(@NoLabel("Named by the title or dates beside it") String assignmentPublicId, String title,
			ReferenceSummary subject,
			LocalDate dueDate) {
	}

	public record LeaveRequest(@NoLabel("Named by the title or dates beside it") String leaveRequestPublicId,
			LocalDate startDate, LocalDate endDate,
			String reason) {
	}
}
