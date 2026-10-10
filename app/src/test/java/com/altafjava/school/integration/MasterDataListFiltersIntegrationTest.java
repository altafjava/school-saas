package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
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
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.filter.AlumniProfileFilter;
import com.altafjava.school.application.filter.AssetFilter;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.filter.EventFilter;
import com.altafjava.school.application.filter.HolidayFilter;
import com.altafjava.school.application.filter.TermFilter;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.AlumniProfileService;
import com.altafjava.school.application.service.AssetService;
import com.altafjava.school.application.service.BoardService;
import com.altafjava.school.application.service.CertificateTypeService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.CurriculumService;
import com.altafjava.school.application.service.DepartmentService;
import com.altafjava.school.application.service.EventService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.application.service.GradingScaleService;
import com.altafjava.school.application.service.HolidayService;
import com.altafjava.school.application.service.HostelBuildingService;
import com.altafjava.school.application.service.LeaveTypeService;
import com.altafjava.school.application.service.PeriodService;
import com.altafjava.school.application.service.RoomService;
import com.altafjava.school.application.service.RouteService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.application.service.VehicleService;
import com.altafjava.school.application.service.VenueService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.alumni.model.AlumniProfile;
import com.altafjava.school.domain.alumni.repository.AlumniProfileRepository;
import com.altafjava.school.domain.certificate.model.CertificateType;
import com.altafjava.school.domain.certificate.repository.CertificateTypeRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.curriculum.model.Board;
import com.altafjava.school.domain.curriculum.model.Curriculum;
import com.altafjava.school.domain.curriculum.model.GradingScale;
import com.altafjava.school.domain.curriculum.repository.BoardRepository;
import com.altafjava.school.domain.curriculum.repository.CurriculumRepository;
import com.altafjava.school.domain.curriculum.repository.GradingScaleRepository;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.department.repository.DepartmentRepository;
import com.altafjava.school.domain.event.model.Event;
import com.altafjava.school.domain.event.repository.EventRepository;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.holiday.model.Holiday;
import com.altafjava.school.domain.holiday.repository.HolidayRepository;
import com.altafjava.school.domain.hostel.model.HostelBuilding;
import com.altafjava.school.domain.hostel.model.Room;
import com.altafjava.school.domain.hostel.repository.HostelBuildingRepository;
import com.altafjava.school.domain.hostel.repository.RoomRepository;
import com.altafjava.school.domain.inventory.model.Asset;
import com.altafjava.school.domain.inventory.model.AssetStatus;
import com.altafjava.school.domain.inventory.repository.AssetRepository;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.leave.repository.LeaveTypeRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.Venue;
import com.altafjava.school.domain.timetable.model.VenueType;
import com.altafjava.school.domain.timetable.repository.PeriodRepository;
import com.altafjava.school.domain.timetable.repository.VenueRepository;
import com.altafjava.school.domain.transport.model.Route;
import com.altafjava.school.domain.transport.model.Vehicle;
import com.altafjava.school.domain.transport.repository.RouteRepository;
import com.altafjava.school.domain.transport.repository.VehicleRepository;
import com.altafjava.school.util.TestPrincipals;

/**
 * The {@code q} search and the extra filters of the master-data lists: each query returns the matching rows
 * of the current tenant and nothing else, and filters on references and dates behave as documented.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MasterDataListFiltersIntegrationTest extends SchoolIntegrationTestBase {

	private static final Pageable PAGE = PageRequest.of(0, 50);

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private DepartmentService departmentService;
	@Autowired
	private DepartmentRepository departmentRepository;
	@Autowired
	private VenueService venueService;
	@Autowired
	private VenueRepository venueRepository;
	@Autowired
	private RouteService routeService;
	@Autowired
	private RouteRepository routeRepository;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private VehicleRepository vehicleRepository;
	@Autowired
	private HostelBuildingService hostelBuildingService;
	@Autowired
	private HostelBuildingRepository hostelBuildingRepository;
	@Autowired
	private RoomService roomService;
	@Autowired
	private RoomRepository roomRepository;
	@Autowired
	private EventService eventService;
	@Autowired
	private EventRepository eventRepository;
	@Autowired
	private AssetService assetService;
	@Autowired
	private AssetRepository assetRepository;
	@Autowired
	private BoardService boardService;
	@Autowired
	private BoardRepository boardRepository;
	@Autowired
	private CurriculumService curriculumService;
	@Autowired
	private CurriculumRepository curriculumRepository;
	@Autowired
	private GradingScaleService gradingScaleService;
	@Autowired
	private GradingScaleRepository gradingScaleRepository;
	@Autowired
	private FeeStructureService feeStructureService;
	@Autowired
	private FeeStructureRepository feeStructureRepository;
	@Autowired
	private LeaveTypeService leaveTypeService;
	@Autowired
	private LeaveTypeRepository leaveTypeRepository;
	@Autowired
	private CertificateTypeService certificateTypeService;
	@Autowired
	private CertificateTypeRepository certificateTypeRepository;
	@Autowired
	private HolidayService holidayService;
	@Autowired
	private HolidayRepository holidayRepository;
	@Autowired
	private TermService termService;
	@Autowired
	private TermRepository termRepository;
	@Autowired
	private AcademicYearService academicYearService;
	@Autowired
	private AcademicYearRepository academicYearRepository;
	@Autowired
	private PeriodService periodService;
	@Autowired
	private PeriodRepository periodRepository;
	@Autowired
	private ClassroomService classroomService;
	@Autowired
	private ClassroomRepository classroomRepository;
	@Autowired
	private AlumniProfileService alumniProfileService;
	@Autowired
	private AlumniProfileRepository alumniProfileRepository;
	@Autowired
	private StudentRepository studentRepository;

	private Tenant tenant;
	private Tenant otherTenant;
	private String tag;

	@BeforeAll
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenant = onboardingService.registerTenant(new RegisterTenantCommand("Master School", "md-a-" + suffix, 1L,
				"admin@md-a.test", "Password123!", "USD"));
		otherTenant = onboardingService.registerTenant(new RegisterTenantCommand("Other School", "md-b-" + suffix,
				1L, "admin@md-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@BeforeEach
	void activateTenant() {
		activate(tenant);
		TestPrincipals.authenticateAsTenantAdmin();
		tag = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
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

	private String alpha() {
		return tag + "-alpha";
	}

	private String beta() {
		return tag + "-beta";
	}

	private static <T> void assertLabels(Page<T> page, Function<T, String> label, String... expected) {
		assertEquals(List.of(expected), page.getContent().stream().map(label).sorted().toList());
	}

	// ----------------------------------------------------------------------- q

	@Test
	void departments_matchQOnNameOrCode() {
		departmentRepository.save(Department.create(alpha() + " Science", "DA" + tag, null));
		departmentRepository.save(Department.create(beta() + " Arts", "DB" + tag, null));

		assertLabels(departmentService.list(alpha(), PAGE), Department::getName, alpha() + " Science");
		assertLabels(departmentService.list("db" + tag.toLowerCase(Locale.ROOT), PAGE), Department::getName,
				beta() + " Arts");
		assertEquals(2, departmentService.list(tag, PAGE).getTotalElements());
	}

	@Test
	void blankQ_matchesEverythingAndWildcardsAreLiteral() {
		departmentRepository.save(Department.create(alpha() + " Science", "DA" + tag, null));

		assertEquals(departmentService.list(null, PAGE).getTotalElements(),
				departmentService.list("   ", PAGE).getTotalElements());
		assertEquals(0, departmentService.list("%" + tag + "%", PAGE).getTotalElements());
	}

	@Test
	void q_doesNotLeakAnotherTenantsRows() {
		activate(otherTenant);
		departmentRepository.save(Department.create(alpha() + " Foreign", "DF" + tag, null));
		activate(tenant);

		assertEquals(0, departmentService.list(alpha(), PAGE).getTotalElements());
	}

	@Test
	void venues_matchQOnNameOrCode() {
		venueRepository.save(Venue.create("VA" + tag, alpha() + " Hall", VenueType.HALL, 100));
		venueRepository.save(Venue.create("VB" + tag, beta() + " Lab", VenueType.LABORATORY, 30));

		assertLabels(venueService.list(alpha(), PAGE), Venue::getName, alpha() + " Hall");
		assertLabels(venueService.list("vb" + tag.toLowerCase(Locale.ROOT), PAGE), Venue::getName, beta() + " Lab");
	}

	@Test
	void routes_matchQOnNameOrCode() {
		routeRepository.save(Route.create(alpha() + " North", "RA" + tag, null));
		routeRepository.save(Route.create(beta() + " South", "RB" + tag, null));

		assertLabels(routeService.list(alpha(), PAGE), Route::getName, alpha() + " North");
	}

	@Test
	void vehicles_matchQOnRegistrationOrDriver() {
		vehicleRepository.save(Vehicle.create("REG-" + alpha(), 40, "Driver One", "+14155550001"));
		vehicleRepository.save(Vehicle.create("REG-" + beta(), 40, "Driver " + alpha(), "+14155550002"));
		vehicleRepository.save(Vehicle.create("REG-" + tag + "-other", 40, "Someone", "+14155550003"));

		assertLabels(vehicleService.list(alpha(), PAGE), Vehicle::getRegistrationNumber, "REG-" + alpha(),
				"REG-" + beta());
	}

	@Test
	void hostelBuildingsAndRooms_matchQ() {
		HostelBuilding north = hostelBuildingRepository.save(HostelBuilding.create(alpha() + " Block", "North"));
		hostelBuildingRepository.save(HostelBuilding.create(beta() + " Block", "South"));
		roomRepository.save(Room.create(north.getId(), "A-" + tag + "-101", 4));
		roomRepository.save(Room.create(north.getId(), "A-" + tag + "-202", 4));

		assertLabels(hostelBuildingService.list(alpha(), PAGE), HostelBuilding::getName, alpha() + " Block");
		assertLabels(roomService.listForBuilding(north.getPublicId().toString(), "101", PAGE), Room::getRoomNumber,
				"A-" + tag + "-101");
		assertEquals(2, roomService.listForBuilding(north.getPublicId().toString(), null, PAGE).getTotalElements());
	}

	@Test
	void boardsCurriculaGradingScalesAndPeriods_matchQ() {
		Board board = boardRepository.save(Board.create(alpha() + " Board", "BA" + tag, null));
		boardRepository.save(Board.create(beta() + " Board", "BB" + tag, null));
		curriculumRepository.save(Curriculum.create(board.getId(), alpha() + " Curriculum", "CA" + tag, null));
		curriculumRepository.save(Curriculum.create(board.getId(), beta() + " Curriculum", "CB" + tag, null));
		gradingScaleRepository.save(GradingScale.create(alpha() + " Scale", false));
		gradingScaleRepository.save(GradingScale.create(beta() + " Scale", false));
		periodRepository.save(Period.create(alpha() + " Period", LocalTime.of(9, 0), LocalTime.of(9, 45), 90));
		periodRepository.save(Period.create(beta() + " Period", LocalTime.of(10, 0), LocalTime.of(10, 45), 91));

		assertLabels(boardService.list(alpha(), PAGE), Board::getName, alpha() + " Board");
		assertLabels(curriculumService.list(alpha(), PAGE), Curriculum::getName, alpha() + " Curriculum");
		assertLabels(gradingScaleService.list(alpha(), PAGE), GradingScale::getName, alpha() + " Scale");
		assertLabels(periodService.listPeriods(alpha(), PAGE), Period::getName, alpha() + " Period");
	}

	@Test
	void feeStructuresLeaveTypesAndCertificateTypes_matchQ() {
		feeStructureRepository.save(FeeStructure.create(alpha() + " Tuition", BigDecimal.TEN, FeeFrequency.MONTHLY,
				null));
		feeStructureRepository.save(FeeStructure.create(beta() + " Transport", BigDecimal.TEN,
				FeeFrequency.MONTHLY, null));
		leaveTypeRepository.save(LeaveType.create(alpha() + " Leave", BigDecimal.TEN));
		leaveTypeRepository.save(LeaveType.create(beta() + " Leave", BigDecimal.TEN));
		certificateTypeRepository.save(CertificateType.create("CA_" + tag, alpha() + " Certificate", "Wording"));
		certificateTypeRepository.save(CertificateType.create("CB_" + tag, beta() + " Certificate", "Wording"));

		assertLabels(feeStructureService.listFeeStructures(alpha(), PAGE), FeeStructure::getName,
				alpha() + " Tuition");
		assertLabels(leaveTypeService.list(alpha(), PAGE), LeaveType::getName, alpha() + " Leave");
		assertLabels(certificateTypeService.list(alpha(), PAGE), CertificateType::getName,
				alpha() + " Certificate");
		assertLabels(certificateTypeService.list("cb_" + tag.toLowerCase(Locale.ROOT), PAGE),
				CertificateType::getName, beta() + " Certificate");
	}

	@Test
	void academicYears_matchQ() {
		academicYearRepository.save(AcademicYear.create(alpha() + " Year", LocalDate.of(2000, 6, 1),
				LocalDate.of(2001, 5, 31), false));
		academicYearRepository.save(AcademicYear.create(beta() + " Year", LocalDate.of(2001, 6, 1),
				LocalDate.of(2002, 5, 31), false));

		assertLabels(academicYearService.listAcademicYears(alpha(), PAGE), AcademicYear::getName,
				alpha() + " Year");
	}

	// ------------------------------------------------------------------- extras

	@Test
	void events_filterByDateWindowAndQ() {
		eventRepository.save(Event.create(alpha() + " Sports Day", null, LocalDateTime.of(2031, 3, 10, 9, 0),
				tag + "-Ground", false, null));
		eventRepository.save(Event.create(alpha() + " Fete", null, LocalDateTime.of(2031, 4, 20, 9, 0), "Hall",
				false, null));

		assertLabels(eventService.list(new EventFilter(new DateWindow(LocalDate.of(2031, 3, 1),
				LocalDate.of(2031, 3, 31)), alpha()), PAGE), Event::getTitle, alpha() + " Sports Day");
		// The last day of the window includes events that day at any hour.
		assertLabels(eventService.list(new EventFilter(new DateWindow(LocalDate.of(2031, 4, 20),
				LocalDate.of(2031, 4, 20)), alpha()), PAGE), Event::getTitle, alpha() + " Fete");
		// q also searches the location.
		assertLabels(eventService.list(new EventFilter(DateWindow.UNBOUNDED, tag + "-ground"), PAGE), Event::getTitle,
				alpha() + " Sports Day");
	}

	@Test
	void assets_filterByStatusAndQ() {
		Asset available = assetRepository.save(Asset.create("AS-" + tag + "-1", alpha() + " Projector", "AV",
				LocalDate.of(2025, 1, 1), BigDecimal.TEN, "Room 1"));
		Asset disposed = assetRepository.save(Asset.create("AS-" + tag + "-2", alpha() + " Old Projector", "AV",
				LocalDate.of(2015, 1, 1), BigDecimal.TEN, "Store"));
		disposed.markDisposed();
		assetRepository.save(disposed);

		assertEquals(2, assetService.list(new AssetFilter(null, alpha()), PAGE).getTotalElements());
		assertLabels(assetService.list(new AssetFilter(AssetStatus.DISPOSED, alpha()), PAGE), Asset::getName,
				alpha() + " Old Projector");
		assertLabels(assetService.list(new AssetFilter(AssetStatus.AVAILABLE, "as-" + tag.toLowerCase(Locale.ROOT)),
				PAGE), Asset::getName, available.getName());
	}

	@Test
	void holidays_filterByDateWindowAndQ() {
		holidayRepository.save(Holiday.create(LocalDate.of(2032, 1, 26), alpha() + " Republic Day", false));
		holidayRepository.save(Holiday.create(LocalDate.of(2032, 8, 15), alpha() + " Independence Day", false));

		assertLabels(holidayService.list(new HolidayFilter(new DateWindow(LocalDate.of(2032, 1, 1),
				LocalDate.of(2032, 3, 31)), alpha()), PAGE), Holiday::getName, alpha() + " Republic Day");
		assertLabels(holidayService.list(new HolidayFilter(new DateWindow(LocalDate.of(2032, 8, 15), null),
				alpha()), PAGE), Holiday::getName, alpha() + " Independence Day");
		assertThrows(IllegalArgumentException.class,
				() -> new DateWindow(LocalDate.of(2032, 8, 15), LocalDate.of(2032, 1, 1)));
	}

	@Test
	void terms_filterByAcademicYearAndQ() {
		AcademicYear yearOne = academicYearRepository.save(AcademicYear.create(alpha() + " Y1",
				LocalDate.of(2003, 6, 1), LocalDate.of(2004, 5, 31), false));
		AcademicYear yearTwo = academicYearRepository.save(AcademicYear.create(beta() + " Y2",
				LocalDate.of(2004, 6, 1), LocalDate.of(2005, 5, 31), false));
		termRepository.save(Term.create(alpha() + " Term 1", LocalDate.of(2003, 6, 1), LocalDate.of(2003, 9, 30),
				yearOne.getId()));
		termRepository.save(Term.create(alpha() + " Term 2", LocalDate.of(2004, 6, 1), LocalDate.of(2004, 9, 30),
				yearTwo.getId()));

		assertLabels(termService.listTerms(new TermFilter(yearOne.getPublicId().toString(), null), PAGE),
				Term::getName, alpha() + " Term 1");
		assertLabels(termService.listTerms(new TermFilter(yearTwo.getPublicId().toString(), alpha()), PAGE),
				Term::getName, alpha() + " Term 2");
		assertEquals(0, termService.listTerms(new TermFilter(yearTwo.getPublicId().toString(), "no-such"), PAGE)
				.getTotalElements());
	}

	@Test
	void classrooms_filterByAcademicYearGradeAndQ() {
		AcademicYear yearOne = academicYearRepository.save(AcademicYear.create(alpha() + " CY1",
				LocalDate.of(2006, 6, 1), LocalDate.of(2007, 5, 31), false));
		AcademicYear yearTwo = academicYearRepository.save(AcademicYear.create(beta() + " CY2",
				LocalDate.of(2007, 6, 1), LocalDate.of(2008, 5, 31), false));
		classroomRepository.save(Classroom.create("C1-" + tag, "Grade " + tag, "A", yearOne.getId(),
				yearOne.getName(), null));
		classroomRepository.save(Classroom.create("C2-" + tag, "Grade " + tag, "B", yearTwo.getId(),
				yearTwo.getName(), null));
		classroomRepository.save(Classroom.create("C3-" + tag, "Other " + tag, "A", yearTwo.getId(),
				yearTwo.getName(), null));

		assertLabels(classroomService.searchClassrooms(PAGE, yearTwo.getPublicId().toString(), null, null),
				Classroom::getClassCode, "C2-" + tag, "C3-" + tag);
		assertLabels(classroomService.searchClassrooms(PAGE, yearTwo.getPublicId().toString(), "Grade " + tag, null),
				Classroom::getClassCode, "C2-" + tag);
		assertLabels(classroomService.searchClassrooms(PAGE, null, "Grade " + tag, "grade " + tag + " a"),
				Classroom::getClassCode, "C1-" + tag);
	}

	@Test
	void alumni_filterByGraduationYearActiveFlagAndQ() {
		Student first = studentRepository.save(Student.create("AL1-" + tag, "Al", "One", null, null));
		Student second = studentRepository.save(Student.create("AL2-" + tag, "Al", "Two", null, null));
		alumniProfileRepository.save(AlumniProfile.create(first.getId(), 2031, alpha() + " Engineer", "a@x.test",
				null));
		AlumniProfile older = alumniProfileRepository.save(AlumniProfile.create(second.getId(), 2020,
				alpha() + " Doctor", "b@x.test", null));
		older.deactivate();
		alumniProfileRepository.save(older);

		assertEquals(2, alumniProfileService.list(new AlumniProfileFilter(null, null, alpha()), PAGE)
				.getTotalElements());
		assertEquals(1, alumniProfileService.list(new AlumniProfileFilter(2031, null, alpha()), PAGE)
				.getTotalElements());
		assertEquals(1, alumniProfileService.list(new AlumniProfileFilter(null, false, alpha()), PAGE)
				.getTotalElements());
		assertLabels(alumniProfileService.list(new AlumniProfileFilter(null, true, alpha() + " eng"), PAGE),
				AlumniProfile::getCurrentOccupation, alpha() + " Engineer");
	}
}
