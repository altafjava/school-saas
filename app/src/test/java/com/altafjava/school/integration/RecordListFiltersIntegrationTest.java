package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.filter.AttendanceFilter;
import com.altafjava.school.application.filter.BookReservationFilter;
import com.altafjava.school.application.filter.CirculationFilter;
import com.altafjava.school.application.filter.CounselingReferralFilter;
import com.altafjava.school.application.filter.CounselingSessionFilter;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.filter.DisciplineIncidentFilter;
import com.altafjava.school.application.filter.FeePaymentFilter;
import com.altafjava.school.application.filter.LeaveRequestFilter;
import com.altafjava.school.application.filter.MedicalIncidentFilter;
import com.altafjava.school.application.filter.PayslipFilter;
import com.altafjava.school.application.filter.TimetableEntryFilter;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.application.service.BookReservationService;
import com.altafjava.school.application.service.CirculationService;
import com.altafjava.school.application.service.CounselingReferralService;
import com.altafjava.school.application.service.CounselingSessionService;
import com.altafjava.school.application.service.DisciplineIncidentService;
import com.altafjava.school.application.service.EmployeeService;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.LeaveRequestService;
import com.altafjava.school.application.service.MedicalIncidentService;
import com.altafjava.school.application.service.PayslipService;
import com.altafjava.school.application.service.PeriodAttendanceService;
import com.altafjava.school.application.service.SalaryStructureService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.application.service.TicketService;
import com.altafjava.school.application.service.TimetableService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.counseling.model.CounselingReferral;
import com.altafjava.school.domain.counseling.model.CounselingReferralStatus;
import com.altafjava.school.domain.counseling.model.CounselingSession;
import com.altafjava.school.domain.counseling.repository.CounselingReferralRepository;
import com.altafjava.school.domain.counseling.repository.CounselingSessionRepository;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.discipline.model.DisciplineIncident;
import com.altafjava.school.domain.discipline.model.IncidentSeverity;
import com.altafjava.school.domain.discipline.repository.DisciplineIncidentRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmploymentType;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.model.PaymentSource;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.health.model.MedicalIncident;
import com.altafjava.school.domain.health.repository.MedicalIncidentRepository;
import com.altafjava.school.domain.helpdesk.model.Ticket;
import com.altafjava.school.domain.helpdesk.model.TicketCategory;
import com.altafjava.school.domain.helpdesk.repository.TicketRepository;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;
import com.altafjava.school.domain.library.model.Book;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.model.BookReservation;
import com.altafjava.school.domain.library.model.Circulation;
import com.altafjava.school.domain.library.model.ReservationStatus;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.BookRepository;
import com.altafjava.school.domain.library.repository.BookReservationRepository;
import com.altafjava.school.domain.library.repository.CirculationRepository;
import com.altafjava.school.domain.payroll.model.Payslip;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import com.altafjava.school.util.TestPrincipals;

/**
 * The filters of the record lists: each filter keeps only the matching rows of the current tenant, filters
 * combine, and a reference to a record that does not exist is a 404 instead of silently matching nothing.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordListFiltersIntegrationTest extends SchoolIntegrationTestBase {

	private static final Pageable PAGE = PageRequest.of(0, 50);

	@Autowired
	private TenantOnboardingService onboardingService;
	@Autowired
	private StudentRepository studentRepository;
	@Autowired
	private TeacherService teacherService;
	@Autowired
	private TeacherRepository teacherRepository;
	@Autowired
	private EmployeeService employeeService;
	@Autowired
	private DepartmentRepository departmentRepository;
	@Autowired
	private AcademicYearRepository academicYearRepository;
	@Autowired
	private ClassroomRepository classroomRepository;
	@Autowired
	private SubjectRepository subjectRepository;
	@Autowired
	private PeriodRepository periodRepository;
	@Autowired
	private TimetableEntryRepository timetableEntryRepository;
	@Autowired
	private TimetableService timetableService;
	@Autowired
	private CounselingSessionRepository counselingSessionRepository;
	@Autowired
	private CounselingSessionService counselingSessionService;
	@Autowired
	private CounselingReferralRepository counselingReferralRepository;
	@Autowired
	private CounselingReferralService counselingReferralService;
	@Autowired
	private DisciplineIncidentRepository disciplineIncidentRepository;
	@Autowired
	private DisciplineIncidentService disciplineIncidentService;
	@Autowired
	private MedicalIncidentRepository medicalIncidentRepository;
	@Autowired
	private MedicalIncidentService medicalIncidentService;
	@Autowired
	private BookRepository bookRepository;
	@Autowired
	private BookCopyRepository bookCopyRepository;
	@Autowired
	private BookReservationRepository bookReservationRepository;
	@Autowired
	private BookReservationService bookReservationService;
	@Autowired
	private CirculationRepository circulationRepository;
	@Autowired
	private CirculationService circulationService;
	@Autowired
	private FeeStructureRepository feeStructureRepository;
	@Autowired
	private FeePaymentRepository feePaymentRepository;
	@Autowired
	private FeePaymentService feePaymentService;
	@Autowired
	private LeaveTypeRepository leaveTypeRepository;
	@Autowired
	private LeaveRequestRepository leaveRequestRepository;
	@Autowired
	private LeaveRequestService leaveRequestService;
	@Autowired
	private AdmissionRepository admissionRepository;
	@Autowired
	private AdmissionService admissionService;
	@Autowired
	private TicketRepository ticketRepository;
	@Autowired
	private TicketService ticketService;
	@Autowired
	private SalaryStructureService salaryStructureService;
	@Autowired
	private PayslipService payslipService;
	@Autowired
	private PeriodAttendanceService periodAttendanceService;

	private Tenant tenant;
	private String tag;
	private int sequence;

	@BeforeAll
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenant = onboardingService.registerTenant(new RegisterTenantCommand("Records School", "rec-a-" + suffix,
				1L, "admin@rec-a.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@BeforeEach
	void activateTenant() {
		activate(tenant);
		TestPrincipals.authenticateAsTenantAdmin();
		tag = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
		sequence = 0;
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		TestPrincipals.clear();
	}

	private void activate(Tenant target) {
		TenantContext.ForTesting.setCurrentTenant(target.getId(), target.getPublicId(), target.getSubdomain(),
				target.getType());
	}

	private Student student() {
		return studentRepository
				.save(Student.create("S-" + tag + "-" + (++sequence), "Stu", "Dent" + sequence, null, null));
	}

	private Teacher teacher() {
		return teacherService.hire("T-" + tag + "-" + (++sequence), "Tea", "Cher" + sequence,
				"t" + sequence + "-" + tag.toLowerCase(Locale.ROOT) + "@school.test", LocalDate.of(2020, 1, 1));
	}

	private static String id(Student student) {
		return student.getPublicId().toString();
	}

	private static <T> void assertIds(Page<T> page, Function<T, Object> key, Object... expected) {
		assertEquals(List.of(expected).stream().map(Object::toString).sorted().toList(),
				page.getContent().stream().map(key).map(Object::toString).sorted().toList());
	}

	// ----------------------------------------------------- incidents and counselling

	@Test
	void counselingSessions_filterByStudentFollowUpAndDate() {
		Student first = student();
		Student second = student();
		Teacher counselor = teacher();
		CounselingSession early = counselingSessionRepository.save(CounselingSession.schedule(first.getId(),
				counselor.getId(), LocalDate.of(2031, 1, 10), "n", true));
		CounselingSession late = counselingSessionRepository.save(CounselingSession.schedule(second.getId(),
				counselor.getId(), LocalDate.of(2031, 2, 10), "n", false));

		assertIds(counselingSessionService.listAll(
				new CounselingSessionFilter(id(first), DateWindow.UNBOUNDED, null), PAGE), CounselingSession::getId,
				early.getId());
		assertIds(counselingSessionService.listAll(new CounselingSessionFilter(null, new DateWindow(
				LocalDate.of(2031, 2, 1), LocalDate.of(2031, 2, 28)), false), PAGE), CounselingSession::getId,
				late.getId());
		assertEquals(0, counselingSessionService.listAll(new CounselingSessionFilter(id(first),
				new DateWindow(LocalDate.of(2031, 2, 1), null), null), PAGE).getTotalElements());
	}

	@Test
	void counselingReferrals_filterByStudentStatusAndDay() {
		Student first = student();
		Student second = student();
		CounselingReferral pending = counselingReferralRepository
				.save(CounselingReferral.refer(first.getId(), 1L, "Anxiety"));
		CounselingReferral declined = CounselingReferral.refer(second.getId(), 1L, "Conduct");
		declined.decline();
		declined = counselingReferralRepository.save(declined);
		LocalDate today = LocalDate.now();

		assertIds(counselingReferralService.listAll(new CounselingReferralFilter(id(first), null,
				DateWindow.UNBOUNDED), PAGE), CounselingReferral::getId, pending.getId());
		assertIds(counselingReferralService.listAll(new CounselingReferralFilter(id(second),
				CounselingReferralStatus.DECLINED, new DateWindow(today, today)), PAGE), CounselingReferral::getId,
				declined.getId());
		assertEquals(0, counselingReferralService.listAll(new CounselingReferralFilter(id(second),
				CounselingReferralStatus.PENDING, DateWindow.UNBOUNDED), PAGE).getTotalElements());
		assertEquals(0, counselingReferralService.listAll(new CounselingReferralFilter(id(first), null,
				new DateWindow(null, today.minusDays(1))), PAGE).getTotalElements());
	}

	@Test
	void disciplineIncidents_filterByStudentSeverityAndDate() {
		Student first = student();
		Student second = student();
		Teacher reporter = teacher();
		DisciplineIncident minor = disciplineIncidentRepository.save(DisciplineIncident.report(first.getId(),
				reporter.getId(), LocalDate.of(2031, 5, 5), IncidentSeverity.MINOR, "Late"));
		DisciplineIncident severe = disciplineIncidentRepository.save(DisciplineIncident.report(second.getId(),
				reporter.getId(), LocalDate.of(2031, 6, 6), IncidentSeverity.SEVERE, "Fight"));

		assertIds(disciplineIncidentService.listAll(new DisciplineIncidentFilter(id(first), null,
				DateWindow.UNBOUNDED), PAGE), DisciplineIncident::getId, minor.getId());
		assertIds(disciplineIncidentService.listAll(new DisciplineIncidentFilter(null, IncidentSeverity.SEVERE,
				new DateWindow(LocalDate.of(2031, 6, 1), LocalDate.of(2031, 6, 30))), PAGE),
				DisciplineIncident::getId, severe.getId());
		assertEquals(0, disciplineIncidentService.listAll(new DisciplineIncidentFilter(id(second),
				IncidentSeverity.MINOR, DateWindow.UNBOUNDED), PAGE).getTotalElements());
	}

	@Test
	void medicalIncidents_filterByStudentAndDay() {
		Student first = student();
		Student second = student();
		MedicalIncident early = medicalIncidentRepository.save(MedicalIncident.record(first.getId(),
				LocalDateTime.of(2031, 7, 1, 23, 30), "Fall", "Ice", 1L));
		MedicalIncident late = medicalIncidentRepository.save(MedicalIncident.record(second.getId(),
				LocalDateTime.of(2031, 7, 2, 0, 15), "Fever", "Rest", 1L));

		assertIds(medicalIncidentService.listAll(new MedicalIncidentFilter(id(first), DateWindow.UNBOUNDED), PAGE),
				MedicalIncident::getId, early.getId());
		// A day window covers the whole day, up to its last minute.
		assertIds(medicalIncidentService.listAll(new MedicalIncidentFilter(null, new DateWindow(
				LocalDate.of(2031, 7, 1), LocalDate.of(2031, 7, 1))), PAGE), MedicalIncident::getId, early.getId());
		assertIds(medicalIncidentService.listAll(new MedicalIncidentFilter(null, new DateWindow(
				LocalDate.of(2031, 7, 2), null)), PAGE), MedicalIncident::getId, late.getId());
	}

	// ------------------------------------------------------------------- library

	@Test
	void bookReservations_filterByBookStudentAndStatus() {
		Student first = student();
		Student second = student();
		Book dune = bookRepository.save(Book.create("978" + tag + "1", "Dune " + tag, "Herbert", "Ace", "Fiction"));
		Book emma = bookRepository.save(Book.create("978" + tag + "2", "Emma " + tag, "Austen", "Penguin", "Classic"));
		BookReservation queued = bookReservationRepository.save(BookReservation.queue(dune.getId(), first.getId()));
		BookReservation cancelled = BookReservation.queue(emma.getId(), second.getId());
		cancelled.cancel();
		cancelled = bookReservationRepository.save(cancelled);

		assertIds(bookReservationService.list(new BookReservationFilter(dune.getPublicId().toString(), null, null),
				PAGE), BookReservation::getId, queued.getId());
		assertIds(bookReservationService.list(new BookReservationFilter(null, id(second), null), PAGE),
				BookReservation::getId, cancelled.getId());
		assertIds(bookReservationService.list(new BookReservationFilter(null, null, ReservationStatus.CANCELLED),
				PAGE), BookReservation::getId, cancelled.getId());
		assertEquals(0, bookReservationService.list(new BookReservationFilter(dune.getPublicId().toString(),
				id(second), null), PAGE).getTotalElements());
	}

	@Test
	void circulations_filterByStudentBookReturnedAndCheckoutDate() {
		Student first = student();
		Student second = student();
		Book dune = bookRepository.save(Book.create("978" + tag + "3", "Dune " + tag, "Herbert", "Ace", "Fiction"));
		Book emma = bookRepository.save(Book.create("978" + tag + "4", "Emma " + tag, "Austen", "Penguin", "Classic"));
		BookCopy duneCopy = bookCopyRepository.save(BookCopy.create(dune.getId(), "C-" + tag + "-1"));
		BookCopy emmaCopy = bookCopyRepository.save(BookCopy.create(emma.getId(), "C-" + tag + "-2"));
		Circulation out = circulationRepository.save(Circulation.checkout(duneCopy.getId(), first.getId(),
				LocalDate.of(2031, 1, 5), LocalDate.of(2031, 1, 19)));
		Circulation back = Circulation.checkout(emmaCopy.getId(), second.getId(), LocalDate.of(2031, 2, 5),
				LocalDate.of(2031, 2, 19));
		back.returnBook(LocalDate.of(2031, 2, 10), BigDecimal.ZERO);
		back = circulationRepository.save(back);

		assertIds(circulationService.list(new CirculationFilter(id(first), null, null, DateWindow.UNBOUNDED), PAGE),
				Circulation::getId, out.getId());
		assertIds(circulationService.list(new CirculationFilter(null, emma.getPublicId().toString(), null,
				DateWindow.UNBOUNDED), PAGE), Circulation::getId, back.getId());
		assertIds(circulationService.list(new CirculationFilter(id(first), null, false, DateWindow.UNBOUNDED),
				PAGE), Circulation::getId, out.getId());
		assertEquals(0, circulationService.list(new CirculationFilter(id(first), null, true, DateWindow.UNBOUNDED),
				PAGE).getTotalElements());
		assertIds(circulationService.list(new CirculationFilter(null, null, null, new DateWindow(
				LocalDate.of(2031, 2, 1), LocalDate.of(2031, 2, 28))), PAGE).map(c -> c), Circulation::getId,
				back.getId());
	}

	// ------------------------------------------------------------------ timetable

	@Test
	void timetableEntries_filterByClassroomTeacherSubjectAndDay() {
		AcademicYear year = academicYearRepository.save(AcademicYear.create("TT-" + tag, LocalDate.of(2009, 6, 1),
				LocalDate.of(2010, 5, 31), false));
		Classroom classOne = classroomRepository.save(Classroom.create("T1-" + tag, "G", "A", year.getId(),
				year.getName(), null));
		Classroom classTwo = classroomRepository.save(Classroom.create("T2-" + tag, "G", "B", year.getId(),
				year.getName(), null));
		Subject maths = subjectRepository.save(Subject.create("M" + tag, "Maths " + tag, null));
		Subject art = subjectRepository.save(Subject.create("A" + tag, "Art " + tag, null));
		Teacher ana = teacher();
		Teacher bob = teacher();
		Period period = periodRepository
				.save(Period.create("P-" + tag, LocalTime.of(9, 0), LocalTime.of(9, 45), 80));
		TimetableEntry mondayMaths = timetableEntryRepository.save(TimetableEntry.create(DayOfWeek.MONDAY,
				period.getId(), classOne.getId(), maths.getId(), ana.getId(), null));
		TimetableEntry tuesdayArt = timetableEntryRepository.save(TimetableEntry.create(DayOfWeek.TUESDAY,
				period.getId(), classTwo.getId(), art.getId(), bob.getId(), null));

		assertIds(timetableService.listEntries(new TimetableEntryFilter(classOne.getPublicId().toString(), null,
				null, null), PAGE), TimetableEntry::getId, mondayMaths.getId());
		assertIds(timetableService.listEntries(new TimetableEntryFilter(null, bob.getPublicId().toString(), null,
				null), PAGE), TimetableEntry::getId, tuesdayArt.getId());
		assertIds(timetableService.listEntries(new TimetableEntryFilter(null, null, maths.getPublicId().toString(),
				null), PAGE), TimetableEntry::getId, mondayMaths.getId());
		assertIds(timetableService.listEntries(new TimetableEntryFilter(classTwo.getPublicId().toString(), null,
				null, DayOfWeek.TUESDAY), PAGE), TimetableEntry::getId, tuesdayArt.getId());
		assertEquals(0, timetableService.listEntries(new TimetableEntryFilter(classTwo.getPublicId().toString(),
				null, null, DayOfWeek.MONDAY), PAGE).getTotalElements());
	}

	// ------------------------------------------------------------------- finance

	@Test
	void feePayments_filterByStudentStructureDatesAndSource() {
		Student first = student();
		Student second = student();
		FeeStructure tuition = feeStructureRepository
				.save(FeeStructure.create("Tuition " + tag, BigDecimal.TEN, FeeFrequency.MONTHLY, null));
		FeeStructure bus = feeStructureRepository
				.save(FeeStructure.create("Bus " + tag, BigDecimal.TEN, FeeFrequency.MONTHLY, null));
		FeePayment manual = feePaymentRepository.save(FeePayment.create(first.getId(), tuition.getId(),
				BigDecimal.ONE, LocalDateTime.of(2031, 4, 30, 23, 59), "R-" + tag + "-1"));
		FeePayment gateway = feePaymentRepository.save(FeePayment.recordFromGateway(second.getId(), bus.getId(),
				BigDecimal.ONE, LocalDateTime.of(2031, 5, 1, 0, 0), "R-" + tag + "-2", "STRIPE", "ch_" + tag));

		assertIds(feePaymentService.listFeePayments(new FeePaymentFilter(id(first), null, DateWindow.UNBOUNDED,
				null), PAGE), FeePayment::getId, manual.getId());
		assertIds(feePaymentService.listFeePayments(new FeePaymentFilter(null, bus.getPublicId().toString(),
				DateWindow.UNBOUNDED, null), PAGE), FeePayment::getId, gateway.getId());
		assertIds(feePaymentService.listFeePayments(new FeePaymentFilter(id(first), null,
				DateWindow.UNBOUNDED, PaymentSource.MANUAL), PAGE), FeePayment::getId, manual.getId());
		assertEquals(0, feePaymentService.listFeePayments(new FeePaymentFilter(id(first), null,
				DateWindow.UNBOUNDED, PaymentSource.GATEWAY), PAGE).getTotalElements());
		// A payment at the last minute of a day belongs to that day; midnight belongs to the next.
		assertIds(feePaymentService.listFeePayments(new FeePaymentFilter(null, null, new DateWindow(
				LocalDate.of(2031, 4, 30), LocalDate.of(2031, 4, 30)), null), PAGE), FeePayment::getId,
				manual.getId());
		assertIds(feePaymentService.listFeePayments(new FeePaymentFilter(null, null, new DateWindow(
				LocalDate.of(2031, 5, 1), null), null), PAGE), FeePayment::getId, gateway.getId());
	}

	@Test
	void leaveRequests_filterByEmployeeTypeStatusAndOverlappingDates() {
		Teacher ana = teacher();
		Teacher bob = teacher();
		AcademicYear year = academicYearRepository.save(AcademicYear.create("LV-" + tag, LocalDate.of(2011, 6, 1),
				LocalDate.of(2012, 5, 31), false));
		LeaveType casual = leaveTypeRepository.save(LeaveType.create("Casual " + tag, BigDecimal.TEN));
		LeaveType sick = leaveTypeRepository.save(LeaveType.create("Sick " + tag, BigDecimal.TEN));
		LeaveRequest anaCasual = leaveRequestRepository.save(LeaveRequest.submit(ana.getId(), casual.getId(),
				year.getId(), LocalDate.of(2031, 3, 10), LocalDate.of(2031, 3, 14), "Trip", BigDecimal.valueOf(5),
				1));
		LeaveRequest bobSick = LeaveRequest.submit(bob.getId(), sick.getId(), year.getId(),
				LocalDate.of(2031, 8, 1), LocalDate.of(2031, 8, 2), "Flu", BigDecimal.valueOf(2), 1);
		bobSick.cancel();
		bobSick = leaveRequestRepository.save(bobSick);

		assertIds(leaveRequestService.listAll(new LeaveRequestFilter(ana.getPublicId().toString(), null, null,
				DateWindow.UNBOUNDED), PAGE), LeaveRequest::getId, anaCasual.getId());
		assertIds(leaveRequestService.listAll(new LeaveRequestFilter(null, sick.getPublicId().toString(), null,
				DateWindow.UNBOUNDED), PAGE), LeaveRequest::getId, bobSick.getId());
		assertIds(leaveRequestService.listAll(new LeaveRequestFilter(null, null, LeaveRequestStatus.CANCELLED,
				new DateWindow(LocalDate.of(2031, 8, 1), null)), PAGE), LeaveRequest::getId, bobSick.getId());
		// A leave that began before the window but runs into it still matches; one that ended before does not.
		assertIds(leaveRequestService.listAll(new LeaveRequestFilter(ana.getPublicId().toString(), null, null,
				new DateWindow(LocalDate.of(2031, 3, 12), LocalDate.of(2031, 3, 20))), PAGE), LeaveRequest::getId,
				anaCasual.getId());
		assertEquals(0, leaveRequestService.listAll(new LeaveRequestFilter(ana.getPublicId().toString(), null, null,
				new DateWindow(LocalDate.of(2031, 3, 15), null)), PAGE).getTotalElements());
		assertEquals(0, leaveRequestService.listAll(new LeaveRequestFilter(ana.getPublicId().toString(), null, null,
				new DateWindow(null, LocalDate.of(2031, 3, 9))), PAGE).getTotalElements());
		assertEquals(0, leaveRequestService.listAll(new LeaveRequestFilter(ana.getPublicId().toString(),
				sick.getPublicId().toString(), null, DateWindow.UNBOUNDED), PAGE).getTotalElements());
	}

	@Test
	void payslips_filterByEmployeeYearMonthAndStatus() {
		Teacher ana = teacher();
		Teacher bob = teacher();
		for (Teacher teacher : List.of(ana, bob)) {
			salaryStructureService.create(teacher.getPublicId().toString(),
					Map.of("BASIC", BigDecimal.valueOf(50000), "HRA", BigDecimal.valueOf(10000), "TRANSPORT",
							BigDecimal.valueOf(2000), "OTHER_ALLOWANCE", BigDecimal.valueOf(500),
							"OTHER_DEDUCTION", BigDecimal.valueOf(1000)),
					LocalDate.of(2026, 1, 1));
		}
		Payslip anaMay = payslipService.generate(ana.getId(), YearMonth.of(2027, 5));
		Payslip anaJune = payslipService.generate(ana.getId(), YearMonth.of(2027, 6));
		Payslip bobMay = payslipService.generate(bob.getId(), YearMonth.of(2027, 5));

		assertIds(payslipService.list(new PayslipFilter(ana.getPublicId().toString(), null, null, null), PAGE),
				Payslip::getId, anaMay.getId(), anaJune.getId());
		assertIds(payslipService.list(new PayslipFilter(ana.getPublicId().toString(), 2027, 6, null), PAGE),
				Payslip::getId, anaJune.getId());
		assertEquals(2, payslipService.list(new PayslipFilter(null, 2027, 5, null), PAGE).getContent().stream()
				.filter(p -> List.of(anaMay.getId(), bobMay.getId()).contains(p.getId())).count());
		assertEquals(0, payslipService.list(new PayslipFilter(ana.getPublicId().toString(), 2027, 5,
				com.altafjava.school.domain.payroll.model.PayslipStatus.DISBURSED), PAGE).getTotalElements());
	}

	// ------------------------------------------------------- people and requests

	@Test
	void employees_filterByDepartment() {
		Department science = departmentRepository.save(Department.create("Science " + tag, "SC" + tag, null));
		Department arts = departmentRepository.save(Department.create("Arts " + tag, "AR" + tag, null));
		Teacher inScience = teacher();
		inScience.assignHrDetails(science.getId(), "Head", null, EmploymentType.FULL_TIME);
		teacherRepository.save(inScience);
		Teacher inArts = teacher();
		inArts.assignHrDetails(arts.getId(), "Teacher", null, EmploymentType.FULL_TIME);
		teacherRepository.save(inArts);

		assertIds(employeeService.search(null, null, science.getPublicId().toString(), null, PAGE), Employee::getId,
				inScience.getId());
		assertIds(employeeService.search(null, null, arts.getPublicId().toString(), "cher", PAGE), Employee::getId,
				inArts.getId());
		assertThrows(ResourceNotFoundException.class,
				() -> employeeService.search(null, null, UUID.randomUUID().toString(), null, PAGE));
	}

	@Test
	void admissions_filterByStatusAndQ() {
		Admission submitted = admissionRepository.save(Admission.submit("Ann" + tag, "Applicant",
				LocalDate.of(2015, 1, 1), "Pat", "Parent", "pat@x.test", null, "Grade 1"));
		Admission review = Admission.submit("Ben" + tag, "Applicant", LocalDate.of(2015, 1, 2), "Pat", "Parent",
				"pat@x.test", null, "Grade 1");
		review.markUnderReview();
		review = admissionRepository.save(review);

		assertIds(admissionService.searchAdmissions(PAGE, AdmissionStatus.UNDER_REVIEW, tag), Admission::getId,
				review.getId());
		assertIds(
				admissionService.searchAdmissions(PAGE, AdmissionStatus.SUBMITTED,
						"ann" + tag.toLowerCase(Locale.ROOT)),
				Admission::getId, submitted.getId());
		assertEquals(2, admissionService.searchAdmissions(PAGE, null, tag).getTotalElements());
	}

	@Test
	void tickets_filterByQOnTheSubject() {
		Ticket projector = ticketRepository
				.save(Ticket.raise(1L, TicketCategory.TECHNICAL, tag + " Projector broken", "Room 4"));
		ticketRepository.save(Ticket.raise(1L, TicketCategory.FEE, tag + " Receipt missing", "May"));

		assertIds(ticketService.search(null, null, null, tag + " projector", PAGE), Ticket::getId,
				projector.getId());
		assertEquals(1, ticketService.search(null, TicketCategory.FEE, null, tag, PAGE).getTotalElements());
		assertEquals(2, ticketService.search(null, null, null, tag, PAGE).getTotalElements());
	}

	@Test
	void periodAttendance_filtersExecuteWithinTheCallersScope() {
		assertEquals(0, periodAttendanceService.listAttendance(new AttendanceFilter(null, null, new DateWindow(
				LocalDate.of(2099, 1, 1), null), null), PAGE).getTotalElements());
	}
}
