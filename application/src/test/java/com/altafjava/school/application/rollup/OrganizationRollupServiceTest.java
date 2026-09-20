package com.altafjava.school.application.rollup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.organization.OrganizationService;
import com.altafjava.platform.application.tenant.TenantFilterSwitcher;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.organization.model.Organization;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeAssignmentRepository;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.rollup.model.AttendanceRollup;
import com.altafjava.school.domain.rollup.model.OrganizationRollupReport;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationRollupServiceTest {

	@Mock
	private OrganizationService organizationService;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private FeeStructureRepository feeStructureRepository;
	@Mock
	private FeePaymentRepository feePaymentRepository;
	@Mock
	private FeeAssignmentRepository feeAssignmentRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private TenantFilterSwitcher tenantFilterSwitcher;

	private OrganizationRollupService rollupService;

	private final UUID organizationPublicId = UUID.randomUUID();
	private final LocalDate periodStart = LocalDate.of(2026, 1, 1);
	private final LocalDate periodEnd = LocalDate.of(2026, 1, 31);

	@BeforeEach
	void setUp() {
		lenient().when(tenantFilterSwitcher.runWithTenantFilter(any(), any(Supplier.class)))
				.thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());
		rollupService = new OrganizationRollupService(organizationService, studentRepository, attendanceRepository,
				feeStructureRepository, feePaymentRepository, feeAssignmentRepository, studentClassroomLinkRepository,
				tenantFilterSwitcher);
	}

	private Organization organization() {
		Organization organization = Organization.create("Acme School Group", "acme", "contact@acme.test", null);
		organization.setId(10L);
		organization.setPublicId(organizationPublicId);
		return organization;
	}

	private Tenant campus(Long id, String name) {
		return Tenant.builder()
				.id(id)
				.publicId(UUID.randomUUID())
				.name(name)
				.subdomain(name.toLowerCase().replace(" ", "-"))
				.type(TenantType.SHARED)
				.organizationId(10L)
				.build();
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "First", "Last", null, null);
		student.setId(id);
		return student;
	}

	private FeeStructure feeStructureWithId(long id, BigDecimal amount) {
		FeeStructure structure = FeeStructure.create("Fee-" + id, amount, FeeFrequency.MONTHLY, "Standard");
		structure.setId(id);
		return structure;
	}

	private void mockAttendance(Long tenantId, long present, long absent, long late, long excused) {
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(eq(tenantId), any(), any(),
				eq(AttendanceStatus.PRESENT))).thenReturn(present);
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(eq(tenantId), any(), any(),
				eq(AttendanceStatus.ABSENT))).thenReturn(absent);
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(eq(tenantId), any(), any(),
				eq(AttendanceStatus.LATE))).thenReturn(late);
		when(attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(eq(tenantId), any(), any(),
				eq(AttendanceStatus.EXCUSED))).thenReturn(excused);
	}

	@Test
	void generate_aggregatesAcrossAllCampusesInOrganization() {
		Tenant campusA = campus(1L, "Campus A");
		Tenant campusB = campus(2L, "Campus B");

		when(organizationService.findByPublicId(organizationPublicId)).thenReturn(Optional.of(organization()));
		when(organizationService.findTenantsInOrganization(organizationPublicId))
				.thenReturn(List.of(campusA, campusB));

		// Campus A: two students in two different classrooms. A CLASSROOM-scoped fee applies only
		// to student 101's classroom; a STUDENT-scoped fee applies only to student 102 directly —
		// proving the total is NOT simply (sum of every fee structure) x (student count), which
		// would double-charge student 101 for the transport fee and student 102 for tuition.
		Student student101 = studentWithId(101L);
		Student student102 = studentWithId(102L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student101, student102));
		when(studentClassroomLinkRepository.findByStudentIdIn(1L, List.of(101L, 102L))).thenReturn(List.of(
				StudentClassroomLink.create(101L, 501L, 1L, LocalDate.of(2025, 6, 1)),
				StudentClassroomLink.create(102L, 502L, 1L, LocalDate.of(2025, 6, 1))));
		FeeStructure tuitionA = feeStructureWithId(1L, BigDecimal.valueOf(100));
		FeeStructure transportA = feeStructureWithId(2L, BigDecimal.valueOf(50));
		when(feeStructureRepository.findAllByTenantId(1L)).thenReturn(List.of(tuitionA, transportA));
		when(feeAssignmentRepository.findByTenantIdAndStudentIdIn(1L, List.of(101L, 102L)))
				.thenReturn(List.of(FeeAssignment.forStudent(2L, 102L)));
		when(feeAssignmentRepository.findByTenantIdAndClassroomIdIn(eq(1L), any()))
				.thenReturn(List.of(FeeAssignment.forClassroom(1L, 501L)));
		when(feePaymentRepository.sumPaidAmountByTenantId(1L)).thenReturn(BigDecimal.valueOf(90));

		// Campus B: one student, one STUDENT-scoped fee.
		Student student201 = studentWithId(201L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 2L))
				.thenReturn(List.of(student201));
		when(studentClassroomLinkRepository.findByStudentIdIn(2L, List.of(201L))).thenReturn(List.of(
				StudentClassroomLink.create(201L, 601L, 1L, LocalDate.of(2025, 6, 1))));
		FeeStructure tuitionB = feeStructureWithId(3L, BigDecimal.valueOf(200));
		when(feeStructureRepository.findAllByTenantId(2L)).thenReturn(List.of(tuitionB));
		when(feeAssignmentRepository.findByTenantIdAndStudentIdIn(2L, List.of(201L)))
				.thenReturn(List.of(FeeAssignment.forStudent(3L, 201L)));
		when(feePaymentRepository.sumPaidAmountByTenantId(2L)).thenReturn(BigDecimal.valueOf(200));

		mockAttendance(1L, 90, 5, 3, 2);
		mockAttendance(2L, 40, 5, 5, 0);

		OrganizationRollupReport report = rollupService.generate(organizationPublicId.toString(), periodStart,
				periodEnd);

		assertEquals(2, report.campuses().size());
		assertEquals(3, report.totals().activeStudentCount());
		assertEquals(new AttendanceRollup(130, 10, 8, 2), report.totals().attendance());
		// Campus A due = 100 (student 101, classroom-scoped tuition) + 50 (student 102,
		// student-scoped transport) = 150, NOT (100+50) x 2 = 300. Campus B due = 200. Total 350.
		assertEquals(BigDecimal.valueOf(350), report.totals().fees().totalDue());
		assertEquals(BigDecimal.valueOf(290), report.totals().fees().totalPaid());
	}

	@Test
	void generate_noCampusesInOrganization_returnsZeroTotals() {
		when(organizationService.findByPublicId(organizationPublicId)).thenReturn(Optional.of(organization()));
		when(organizationService.findTenantsInOrganization(organizationPublicId)).thenReturn(List.of());

		OrganizationRollupReport report = rollupService.generate(organizationPublicId.toString(), periodStart, periodEnd);

		assertEquals(0, report.campuses().size());
		assertEquals(0, report.totals().activeStudentCount());
		assertEquals(AttendanceRollup.ZERO, report.totals().attendance());
	}

	@Test
	void generate_organizationNotFound_throwsResourceNotFoundException() {
		when(organizationService.findByPublicId(organizationPublicId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> rollupService.generate(organizationPublicId.toString(), periodStart, periodEnd));
	}

	@Test
	void generate_toBeforeFrom_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> rollupService.generate(organizationPublicId.toString(), periodEnd, periodStart));
	}

	@Test
	void generate_rangeExceedsMaximum_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> rollupService.generate(organizationPublicId.toString(), LocalDate.of(2020, 1, 1),
						LocalDate.of(2026, 1, 1)));
	}
}
