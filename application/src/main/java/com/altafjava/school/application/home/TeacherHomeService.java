package com.altafjava.school.application.home;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.application.security.TeachingAssignmentResolver;
import com.altafjava.school.application.security.TeachingAssignments;
import com.altafjava.school.application.service.LeaveRequestService;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.ClassroomHeadcount;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.ExamGradeCount;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.StudentLeaveRequest;
import com.altafjava.school.domain.leave.repository.StudentLeaveRequestRepository;
import com.altafjava.school.domain.lms.model.GradingQueueEntry;
import com.altafjava.school.domain.lms.model.SubmissionStatus;
import com.altafjava.school.domain.lms.repository.SubmissionRepository;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;

/** The teacher home screen: one response, drawn only from this academic year's classes. */
@Service
@RequiredArgsConstructor
public class TeacherHomeService {

	private static final int ITEMS_PER_CARD = 10;
	private static final int EXAMS_SCANNED = 200;
	private static final List<SubmissionStatus> UNGRADED = List.of(SubmissionStatus.SUBMITTED, SubmissionStatus.LATE);
	private static final PageRequest FIRST_ITEMS = PageRequest.of(0, ITEMS_PER_CARD);

	private final HomeCallerResolver homeCallerResolver;
	private final HomeDayResolver homeDayResolver;
	private final TeachingAssignmentResolver teachingAssignmentResolver;
	private final TodayTimetableBuilder todayTimetableBuilder;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;
	private final AttendanceRepository attendanceRepository;
	private final SubmissionRepository submissionRepository;
	private final ExamRepository examRepository;
	private final GradeRepository gradeRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentLeaveRequestRepository studentLeaveRequestRepository;
	private final StudentRepository studentRepository;
	private final EmployeeRepository employeeRepository;
	private final LeaveRequestService leaveRequestService;
	private final NotificationService notificationService;

	/** The classes the screen draws on, and how the teacher relates to them. */
	private record Teaching(TeachingAssignments assignments, Map<Long, Classroom> classrooms, Set<Long> homerooms) {
	}

	@Transactional(readOnly = true)
	public TeacherHome home() {
		Long tenantId = TenantContext.getCurrentTenantId();
		HomeCaller caller = homeCallerResolver.current();
		TeachingAssignments assignments = teachingAssignmentResolver.forUser(caller.userId(), tenantId);
		if (!assignments.isTeacher()) {
			throw new AccessDeniedException("No teacher record linked to the current user");
		}
		HomeDay day = homeDayResolver.today(tenantId);
		Teaching teaching = teachingOf(tenantId, assignments, day);
		return new TeacherHome(day.date(), day.schoolDay(),
				caller.holds(SchoolPermissions.TIMETABLE_READ)
						? todayTimetableBuilder.forTeacher(tenantId, assignments.teacherId(), day)
						: null,
				caller.holds(SchoolPermissions.STUDENT_ATTENDANCE_WRITE) ? attendanceToMark(tenantId, teaching, day)
						: null,
				caller.holds(SchoolPermissions.SUBMISSION_GRADE) ? assignmentsToGrade(tenantId, teaching) : null,
				caller.holds(SchoolPermissions.STUDENT_GRADES_WRITE) ? examsNeedingMarks(tenantId, teaching, day)
						: null,
				caller.holds(SchoolPermissions.STUDENT_LEAVE_REVIEW)
						? studentLeaveToReview(tenantId, teaching, caller)
						: null,
				caller.holds(SchoolPermissions.LEAVE_REQUEST_REVIEW) ? staffLeaveToReview(tenantId, caller) : null,
				new Inbox(notificationService.getUnreadCount(caller.userId().toString())));
	}

	// Timetable rows and class-teacher links outlive their year, so a promoted class must be filtered out.
	private Teaching teachingOf(Long tenantId, TeachingAssignments assignments, HomeDay day) {
		Set<Long> classroomIds = assignments.classroomIds();
		if (classroomIds.isEmpty() || !day.hasAcademicYear()) {
			return new Teaching(assignments, Map.of(), Set.of());
		}
		Map<Long, Classroom> current = classroomRepository.findAllByIdInAndTenantId(classroomIds, tenantId).stream()
				.filter(classroom -> day.academicYearId().equals(classroom.getAcademicYearId()))
				.collect(Collectors.toMap(Classroom::getId, Function.identity()));
		Set<Long> homerooms = current.keySet().stream().filter(assignments.homeroomClassroomIds()::contains)
				.collect(Collectors.toUnmodifiableSet());
		return new Teaching(assignments, current, homerooms);
	}

	// The class teacher answers for the daily register, as the attendance reminder assumes.
	private List<ReferenceSummary> attendanceToMark(Long tenantId, Teaching teaching, HomeDay day) {
		if (!day.schoolDay() || teaching.homerooms().isEmpty()) {
			return List.of();
		}
		Set<Long> marked = Set.copyOf(
				attendanceRepository.findClassroomIdsMarkedOn(tenantId, day.date(), teaching.homerooms()));
		return teaching.homerooms().stream().filter(classroomId -> !marked.contains(classroomId))
				.map(teaching.classrooms()::get).map(HomeReferences::of).toList();
	}

	private TeacherHome.AssignmentsToGrade assignmentsToGrade(Long tenantId, Teaching teaching) {
		if (teaching.classrooms().isEmpty()) {
			return new TeacherHome.AssignmentsToGrade(0, List.of());
		}
		Page<GradingQueueEntry> queue = submissionRepository.findGradingQueue(tenantId,
				teaching.assignments().teacherId(), teaching.classrooms().keySet(), UNGRADED, FIRST_ITEMS);
		Map<Long, Subject> subjects = subjects(tenantId, queue.stream().map(GradingQueueEntry::subjectId));
		return new TeacherHome.AssignmentsToGrade(queue.getTotalElements(), queue.stream()
				.map(entry -> new TeacherHome.AssignmentToGrade(entry.assignmentPublicId().toString(), entry.title(),
						HomeReferences.of(teaching.classrooms().get(entry.classroomId())),
						subjectOf(subjects, entry.subjectId()), entry.dueDate(), entry.ungradedCount()))
				.toList());
	}

	private TeacherHome.ExamsNeedingMarks examsNeedingMarks(Long tenantId, Teaching teaching, HomeDay day) {
		List<Exam> open = openExams(tenantId, teaching, day);
		if (open.isEmpty()) {
			return new TeacherHome.ExamsNeedingMarks(0, List.of());
		}
		Map<Long, Long> graded = gradeRepository.countByExam(tenantId, open.stream().map(Exam::getId).toList()).stream()
				.collect(Collectors.toMap(ExamGradeCount::examId, ExamGradeCount::graded));
		Map<Long, Long> roster = studentClassroomLinkRepository
				.countByClassroomIds(tenantId, open.stream().map(Exam::getClassroomId).distinct().toList()).stream()
				.collect(Collectors.toMap(ClassroomHeadcount::classroomId, ClassroomHeadcount::students));
		List<Exam> incomplete = open.stream()
				.filter(exam -> marksMissing(exam, roster, graded) > 0).toList();
		Map<Long, Subject> subjects = subjects(tenantId,
				incomplete.stream().limit(ITEMS_PER_CARD).map(Exam::getSubjectId));
		return new TeacherHome.ExamsNeedingMarks(incomplete.size(), incomplete.stream().limit(ITEMS_PER_CARD)
				.map(exam -> new TeacherHome.ExamNeedingMarks(exam.getPublicId().toString(), exam.getTitle(),
						HomeReferences.of(teaching.classrooms().get(exam.getClassroomId())),
						subjectOf(subjects, exam.getSubjectId()), exam.getScheduledAt(),
						marksMissing(exam, roster, graded)))
				.toList());
	}

	// Only exams this teacher may record marks for, by the same rule GradeService enforces.
	private List<Exam> openExams(Long tenantId, Teaching teaching, HomeDay day) {
		if (teaching.classrooms().isEmpty()) {
			return List.of();
		}
		return examRepository
				.findWithOpenResults(tenantId, teaching.classrooms().keySet(), ExamStatus.CANCELLED, day.now(),
						PageRequest.of(0, EXAMS_SCANNED))
				.stream()
				.filter(exam -> teaching.assignments().teachesSubject(exam.getClassroomId(), exam.getSubjectId()))
				.toList();
	}

	private static long marksMissing(Exam exam, Map<Long, Long> roster, Map<Long, Long> graded) {
		return Math.max(0, roster.getOrDefault(exam.getClassroomId(), 0L) - graded.getOrDefault(exam.getId(), 0L));
	}

	private TeacherHome.StudentLeaveQueue studentLeaveToReview(Long tenantId, Teaching teaching, HomeCaller caller) {
		if (teaching.homerooms().isEmpty()) {
			return new TeacherHome.StudentLeaveQueue(0, List.of());
		}
		Page<StudentLeaveRequest> queue = studentLeaveRequestRepository.findForReview(tenantId,
				LeaveRequestStatus.PENDING, teaching.homerooms(), caller.userId(), FIRST_ITEMS);
		List<Long> studentIds = queue.stream().map(StudentLeaveRequest::getStudentId).distinct().toList();
		Map<Long, Student> students = studentIds.isEmpty() ? Map.of()
				: byId(studentRepository.findAllByIdInAndTenantId(studentIds, tenantId), Student::getId);
		return new TeacherHome.StudentLeaveQueue(queue.getTotalElements(), queue.stream()
				.filter(request -> students.containsKey(request.getStudentId()))
				.map(request -> new TeacherHome.StudentLeaveItem(request.getPublicId().toString(),
						HomeReferences.of(students.get(request.getStudentId())),
						HomeReferences.of(teaching.classrooms().get(request.getClassroomId())), request.getStartDate(),
						request.getEndDate(), request.getReason()))
				.toList());
	}

	private TeacherHome.StaffLeaveQueue staffLeaveToReview(Long tenantId, HomeCaller caller) {
		if (employeeRepository.findByUserIdAndTenantId(caller.userId(), tenantId).isEmpty()) {
			return new TeacherHome.StaffLeaveQueue(0, List.of());
		}
		Page<LeaveRequest> queue = leaveRequestService
				.listAwaitingMyReview(PageRequest.of(0, ITEMS_PER_CARD, Sort.by("startDate", "id")));
		List<Long> employeeIds = queue.stream().map(LeaveRequest::getEmployeeId).distinct().toList();
		Map<Long, Employee> employees = employeeIds.isEmpty() ? Map.of()
				: byId(employeeRepository.findAllByIdInAndTenantId(employeeIds, tenantId), Employee::getId);
		return new TeacherHome.StaffLeaveQueue(queue.getTotalElements(), queue.stream()
				.filter(request -> employees.containsKey(request.getEmployeeId()))
				.map(request -> new TeacherHome.StaffLeaveItem(request.getPublicId().toString(),
						HomeReferences.of(employees.get(request.getEmployeeId())), request.getStartDate(),
						request.getEndDate()))
				.toList());
	}

	private Map<Long, Subject> subjects(Long tenantId, Stream<Long> subjectIds) {
		List<Long> ids = subjectIds.distinct().toList();
		return ids.isEmpty() ? Map.of()
				: byId(subjectRepository.findAllByIdInAndTenantId(ids, tenantId), Subject::getId);
	}

	private static ReferenceSummary subjectOf(Map<Long, Subject> subjects, Long subjectId) {
		Subject subject = subjects.get(subjectId);
		return subject == null ? null : HomeReferences.of(subject);
	}

	private static <T, K> Map<K, T> byId(Collection<T> records, Function<? super T, K> id) {
		return records.stream().collect(Collectors.toMap(id, Function.identity(), (first, duplicate) -> first));
	}
}
