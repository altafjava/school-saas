package com.altafjava.school.application.home;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.AttendanceTally;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.attendance.service.AttendancePercentageCalculator;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.grade.model.PublishedResult;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.StudentLeaveRequest;
import com.altafjava.school.domain.leave.repository.StudentLeaveRequestRepository;
import com.altafjava.school.domain.lms.model.Assignment;
import com.altafjava.school.domain.lms.model.SubmittedWork;
import com.altafjava.school.domain.lms.repository.AssignmentRepository;
import com.altafjava.school.domain.lms.repository.SubmissionRepository;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;

/**
 * Builds every card for any number of students with set-based queries, so a family of five costs
 * the same as a family of one. Rows are loaded in bulk first and shared out per student afterwards.
 */
@Component
@RequiredArgsConstructor
class StudentOverviewLoader {

	private static final int ITEMS_PER_CARD = 5;
	private static final int ROWS_PER_QUERY = 500;
	private static final int UPCOMING_EXAM_DAYS = 30;
	private static final int RECENT_RESULT_DAYS = 90;
	private static final int HOMEWORK_DAYS_BACK = 14;
	private static final int HOMEWORK_DAYS_AHEAD = 14;
	private static final PageRequest ROW_LIMIT = PageRequest.of(0, ROWS_PER_QUERY);

	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;
	private final AttendanceRepository attendanceRepository;
	private final ExamRepository examRepository;
	private final GradeRepository gradeRepository;
	private final AssignmentRepository assignmentRepository;
	private final SubmissionRepository submissionRepository;
	private final StudentLeaveRequestRepository studentLeaveRequestRepository;
	private final FeePaymentService feePaymentService;
	private final AttendancePercentageCalculator attendancePercentageCalculator = new AttendancePercentageCalculator();

	private record Bulk(Map<Long, Long> classroomIdByStudent, Map<Long, Classroom> classrooms,
			Map<Long, Subject> subjects, Map<Long, StudentOverview.Attendance> attendance,
			Map<Long, StudentOverview.Fees> fees, List<Exam> exams, List<PublishedResult> results,
			List<Assignment> assignments, Set<SubmittedWork> submitted, List<StudentLeaveRequest> leaveRequests) {
	}

	List<StudentOverview> load(Long tenantId, List<Student> students, HomeCaller caller, HomeDay day) {
		if (students.isEmpty()) {
			return List.of();
		}
		Bulk bulk = loadBulk(tenantId, students, caller, day);
		return students.stream().map(student -> overviewOf(student, bulk, caller)).toList();
	}

	private Bulk loadBulk(Long tenantId, List<Student> students, HomeCaller caller, HomeDay day) {
		List<Long> studentIds = students.stream().map(Student::getId).toList();
		Map<Long, Long> classroomIdByStudent = currentClassrooms(tenantId, studentIds, day);
		Set<Long> classroomIds = Set.copyOf(classroomIdByStudent.values());
		List<Exam> exams = caller.holds(SchoolPermissions.EXAM_READ) ? upcomingExams(tenantId, classroomIds, day)
				: List.of();
		List<PublishedResult> results = caller.holds(SchoolPermissions.STUDENT_GRADES_READ)
				? recentResults(tenantId, studentIds, day)
				: List.of();
		List<Assignment> assignments = caller.holds(SchoolPermissions.ASSIGNMENT_READ)
				? homework(tenantId, classroomIds, day)
				: List.of();
		return new Bulk(classroomIdByStudent, classrooms(tenantId, classroomIds),
				subjects(tenantId, exams, results, assignments),
				caller.holds(SchoolPermissions.STUDENT_ATTENDANCE_READ) ? attendance(tenantId, studentIds, day)
						: Map.of(),
				caller.holds(SchoolPermissions.STUDENT_FEE_BALANCE_READ) ? fees(tenantId, students, day) : Map.of(),
				exams, results, assignments, submitted(tenantId, studentIds, assignments),
				mayReadLeave(caller) ? pendingLeave(tenantId, studentIds) : List.of());
	}

	private static boolean mayReadLeave(HomeCaller caller) {
		return caller.holds(SchoolPermissions.STUDENT_LEAVE_SELF_SERVICE)
				|| caller.holds(SchoolPermissions.STUDENT_LEAVE_MANAGE);
	}

	private Map<Long, Long> currentClassrooms(Long tenantId, List<Long> studentIds, HomeDay day) {
		if (!day.hasAcademicYear()) {
			return Map.of();
		}
		return studentClassroomLinkRepository
				.findByStudentIdsAndAcademicYearId(tenantId, studentIds, day.academicYearId()).stream()
				.collect(Collectors.toMap(StudentClassroomLink::getStudentId, StudentClassroomLink::getClassroomId,
						(first, duplicate) -> first));
	}

	private Map<Long, Classroom> classrooms(Long tenantId, Set<Long> classroomIds) {
		return classroomIds.isEmpty() ? Map.of()
				: byId(classroomRepository.findAllByIdInAndTenantId(classroomIds, tenantId), Classroom::getId);
	}

	private List<Exam> upcomingExams(Long tenantId, Set<Long> classroomIds, HomeDay day) {
		return classroomIds.isEmpty() ? List.of()
				: examRepository.findPublishedInClassroomsBetween(tenantId, classroomIds, ExamStatus.SCHEDULED,
						day.now(),
						day.now().plusDays(UPCOMING_EXAM_DAYS), ROW_LIMIT);
	}

	private List<PublishedResult> recentResults(Long tenantId, List<Long> studentIds, HomeDay day) {
		Instant since = day.now().minusDays(RECENT_RESULT_DAYS).toInstant(ZoneOffset.UTC);
		return gradeRepository.findPublishedSince(tenantId, studentIds, since, ROW_LIMIT);
	}

	private List<Assignment> homework(Long tenantId, Set<Long> classroomIds, HomeDay day) {
		return classroomIds.isEmpty() ? List.of()
				: assignmentRepository.findDueInClassrooms(tenantId, classroomIds,
						day.date().minusDays(HOMEWORK_DAYS_BACK), day.date().plusDays(HOMEWORK_DAYS_AHEAD), ROW_LIMIT);
	}

	private Set<SubmittedWork> submitted(Long tenantId, List<Long> studentIds, List<Assignment> assignments) {
		return assignments.isEmpty() ? Set.of()
				: Set.copyOf(submissionRepository.findSubmittedWork(tenantId, studentIds,
						assignments.stream().map(Assignment::getId).toList()));
	}

	private List<StudentLeaveRequest> pendingLeave(Long tenantId, List<Long> studentIds) {
		return studentLeaveRequestRepository.findByStudentsAndStatus(tenantId, LeaveRequestStatus.PENDING, studentIds,
				ROW_LIMIT);
	}

	private Map<Long, Subject> subjects(Long tenantId, List<Exam> exams, List<PublishedResult> results,
			List<Assignment> assignments) {
		List<Long> subjectIds = Stream.of(exams.stream().map(Exam::getSubjectId),
				results.stream().map(PublishedResult::subjectId), assignments.stream().map(Assignment::getSubjectId))
				.flatMap(Function.identity()).distinct().toList();
		return subjectIds.isEmpty() ? Map.of()
				: byId(subjectRepository.findAllByIdInAndTenantId(subjectIds, tenantId), Subject::getId);
	}

	private Map<Long, StudentOverview.Attendance> attendance(Long tenantId, List<Long> studentIds, HomeDay day) {
		Map<Long, AttendanceStatus> today = attendanceRepository
				.findAllByTenantIdAndAttendanceDateAndStudentIdIn(tenantId, day.date(), studentIds).stream()
				.collect(Collectors.toMap(Attendance::getStudentId, Attendance::getStatus,
						(first, duplicate) -> first));
		Map<Long, List<AttendanceTally>> tallies = attendanceRepository
				.tallyByStudent(tenantId, studentIds, day.yearStart(), day.date(), day.holidays().isEmpty(),
						day.holidays())
				.stream().collect(Collectors.groupingBy(AttendanceTally::studentId));
		Map<Long, StudentOverview.Attendance> attendance = new HashMap<>();
		for (Long studentId : studentIds) {
			attendance.put(studentId, attendanceOf(today.get(studentId), tallies.getOrDefault(studentId, List.of())));
		}
		return attendance;
	}

	private StudentOverview.Attendance attendanceOf(AttendanceStatus today, List<AttendanceTally> tallies) {
		AttendancePercentage percentage = attendancePercentageCalculator.calculate(AttendanceTally.byStatus(tallies));
		return new StudentOverview.Attendance(today, percentage.presentDays(), percentage.totalMarkedDays(),
				percentage.percentage());
	}

	private Map<Long, StudentOverview.Fees> fees(Long tenantId, List<Student> students, HomeDay day) {
		Map<Long, StudentOverview.Fees> fees = new HashMap<>();
		feePaymentService.calculateBalancesForStudents(tenantId, students)
				.forEach((studentId, balances) -> fees.put(studentId, feesOf(balances, day.date())));
		return fees;
	}

	private static StudentOverview.Fees feesOf(List<FeeBalance> balances, LocalDate today) {
		List<FeeBalance> owing = balances.stream().filter(balance -> balance.outstandingBalance().signum() > 0)
				.toList();
		BigDecimal outstanding = owing.stream().map(FeeBalance::outstandingBalance).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		LocalDate nextDue = owing.stream().map(FeeBalance::dueDate).filter(Objects::nonNull)
				.min(Comparator.naturalOrder()).orElse(null);
		return new StudentOverview.Fees(outstanding, nextDue, nextDue != null && nextDue.isBefore(today));
	}

	private StudentOverview overviewOf(Student student, Bulk bulk, HomeCaller caller) {
		Long classroomId = bulk.classroomIdByStudent().get(student.getId());
		Classroom classroom = classroomId == null ? null : bulk.classrooms().get(classroomId);
		return new StudentOverview(HomeReferences.of(student), classroom == null ? null : HomeReferences.of(classroom),
				bulk.attendance().get(student.getId()), bulk.fees().get(student.getId()),
				caller.holds(SchoolPermissions.EXAM_READ) ? examsOf(classroomId, bulk) : null,
				caller.holds(SchoolPermissions.STUDENT_GRADES_READ) ? resultsOf(student, bulk) : null,
				caller.holds(SchoolPermissions.ASSIGNMENT_READ) ? homeworkOf(student, classroomId, bulk) : null,
				mayReadLeave(caller) ? leaveOf(student, bulk) : null);
	}

	private List<StudentOverview.UpcomingExam> examsOf(Long classroomId, Bulk bulk) {
		return bulk.exams().stream().filter(exam -> exam.getClassroomId().equals(classroomId))
				.limit(ITEMS_PER_CARD)
				.map(exam -> new StudentOverview.UpcomingExam(exam.getPublicId().toString(), exam.getTitle(),
						subject(bulk, exam.getSubjectId()), exam.getScheduledAt()))
				.toList();
	}

	private List<StudentOverview.RecentResult> resultsOf(Student student, Bulk bulk) {
		return bulk.results().stream().filter(result -> result.studentId().equals(student.getId()))
				.limit(ITEMS_PER_CARD)
				.map(result -> new StudentOverview.RecentResult(result.examPublicId().toString(), result.examTitle(),
						subject(bulk, result.subjectId()), result.marks(), result.maxMarks(), result.gradeLetter(),
						result.publishedAt()))
				.toList();
	}

	private List<StudentOverview.Homework> homeworkOf(Student student, Long classroomId, Bulk bulk) {
		return bulk.assignments().stream()
				.filter(assignment -> assignment.getClassroomId().equals(classroomId))
				.filter(assignment -> !bulk.submitted()
						.contains(new SubmittedWork(student.getId(), assignment.getId())))
				.limit(ITEMS_PER_CARD)
				.map(assignment -> new StudentOverview.Homework(assignment.getPublicId().toString(),
						assignment.getTitle(), subject(bulk, assignment.getSubjectId()), assignment.getDueDate()))
				.toList();
	}

	private List<StudentOverview.LeaveRequest> leaveOf(Student student, Bulk bulk) {
		return bulk.leaveRequests().stream().filter(request -> request.getStudentId().equals(student.getId()))
				.limit(ITEMS_PER_CARD)
				.map(request -> new StudentOverview.LeaveRequest(request.getPublicId().toString(),
						request.getStartDate(), request.getEndDate(), request.getReason()))
				.toList();
	}

	private static ReferenceSummary subject(Bulk bulk, Long subjectId) {
		Subject subject = bulk.subjects().get(subjectId);
		return subject == null ? null : HomeReferences.of(subject);
	}

	private static <T, K> Map<K, T> byId(Collection<T> records, Function<? super T, K> id) {
		return records.stream().collect(Collectors.toMap(id, Function.identity(), (first, duplicate) -> first));
	}
}
