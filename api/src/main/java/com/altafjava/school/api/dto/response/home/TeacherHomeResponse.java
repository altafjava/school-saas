package com.altafjava.school.api.dto.response.home;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The teacher home screen. A card the caller's role may not read is absent; each queue shows its first items and the total waiting.")
public record TeacherHomeResponse(@Schema(description = "Today at the school") LocalDate date,
		@Schema(description = "False on Sundays and holidays") boolean schoolDay,
		List<TimetableSlotResponse> timetable,
		@Schema(description = "The caller's classes with no register today") List<ReferenceSummary> attendanceToMark,
		AssignmentsToGrade assignmentsToGrade, ExamsNeedingMarks examsNeedingMarks,
		StudentLeaveQueue studentLeaveToReview, StaffLeaveQueue staffLeaveToReview, InboxResponse inbox) {

	public TeacherHomeResponse {
		timetable = timetable == null ? null : List.copyOf(timetable);
		attendanceToMark = attendanceToMark == null ? null : List.copyOf(attendanceToMark);
	}

	public record AssignmentsToGrade(long total, List<AssignmentToGrade> items) {

		public AssignmentsToGrade {
			items = items == null ? null : List.copyOf(items);
		}
	}

	public record AssignmentToGrade(@NoLabel("Named by the title or dates beside it") String assignmentPublicId,
			String title, ReferenceSummary classroom,
			ReferenceSummary subject, LocalDate dueDate, long ungradedCount) {
	}

	public record ExamsNeedingMarks(long total, List<ExamNeedingMarks> items) {

		public ExamsNeedingMarks {
			items = items == null ? null : List.copyOf(items);
		}
	}

	public record ExamNeedingMarks(@NoLabel("Named by the title or dates beside it") String examPublicId, String title,
			ReferenceSummary classroom,
			ReferenceSummary subject, LocalDateTime scheduledAt, long marksMissing) {
	}

	public record StudentLeaveQueue(long total, List<StudentLeaveItem> items) {

		public StudentLeaveQueue {
			items = items == null ? null : List.copyOf(items);
		}
	}

	public record StudentLeaveItem(@NoLabel("Named by the title or dates beside it") String leaveRequestPublicId,
			ReferenceSummary student,
			ReferenceSummary classroom, LocalDate startDate, LocalDate endDate, String reason) {
	}

	public record StaffLeaveQueue(long total, List<StaffLeaveItem> items) {

		public StaffLeaveQueue {
			items = items == null ? null : List.copyOf(items);
		}
	}

	public record StaffLeaveItem(@NoLabel("Named by the title or dates beside it") String leaveRequestPublicId,
			ReferenceSummary employee, LocalDate startDate,
			LocalDate endDate) {
	}
}
