package com.altafjava.school.application.home;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.altafjava.school.application.reference.ReferenceSummary;

/**
 * A teacher's day. A card the caller's role may not read is absent. Each work queue shows its first
 * items and the {@code total} waiting.
 */
public record TeacherHome(LocalDate date, boolean schoolDay, List<TimetableSlot> timetable,
		List<ReferenceSummary> attendanceToMark, AssignmentsToGrade assignmentsToGrade,
		ExamsNeedingMarks examsNeedingMarks, StudentLeaveQueue studentLeaveToReview, StaffLeaveQueue staffLeaveToReview,
		Inbox inbox) {

	public TeacherHome {
		timetable = timetable == null ? null : List.copyOf(timetable);
		attendanceToMark = attendanceToMark == null ? null : List.copyOf(attendanceToMark);
	}

	public record AssignmentsToGrade(long total, List<AssignmentToGrade> items) {

		public AssignmentsToGrade {
			items = items == null ? null : List.copyOf(items);
		}
	}

	public record AssignmentToGrade(String assignmentPublicId, String title, ReferenceSummary classroom,
			ReferenceSummary subject, LocalDate dueDate, long ungradedCount) {
	}

	public record ExamsNeedingMarks(long total, List<ExamNeedingMarks> items) {

		public ExamsNeedingMarks {
			items = items == null ? null : List.copyOf(items);
		}
	}

	public record ExamNeedingMarks(String examPublicId, String title, ReferenceSummary classroom,
			ReferenceSummary subject, LocalDateTime scheduledAt, long marksMissing) {
	}

	public record StudentLeaveQueue(long total, List<StudentLeaveItem> items) {

		public StudentLeaveQueue {
			items = items == null ? null : List.copyOf(items);
		}
	}

	public record StudentLeaveItem(String leaveRequestPublicId, ReferenceSummary student, ReferenceSummary classroom,
			LocalDate startDate, LocalDate endDate, String reason) {
	}

	public record StaffLeaveQueue(long total, List<StaffLeaveItem> items) {

		public StaffLeaveQueue {
			items = items == null ? null : List.copyOf(items);
		}
	}

	public record StaffLeaveItem(String leaveRequestPublicId, ReferenceSummary employee, LocalDate startDate,
			LocalDate endDate) {
	}
}
